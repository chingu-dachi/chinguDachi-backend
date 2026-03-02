package com.chat.chingudachi.presentation.chat

import com.chat.chingudachi.application.chat.ReadMessagesUseCase
import com.chat.chingudachi.application.chat.SendMessageUseCase
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.common.BusinessException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal
import java.time.Instant

private val log = KotlinLogging.logger {}

@Controller
class ChatMessageHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val readMessagesUseCase: ReadMessagesUseCase,
    private val roomMemberStore: RoomMemberStore,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    @MessageMapping("/chat.send")
    fun handleSend(payload: SendMessagePayload, principal: Principal) {
        val accountId = principal.name.toLong()
        val result = sendMessageUseCase.execute(accountId, payload.roomId, payload.text)

        val event = ChatEvent(
            type = ChatEventType.MESSAGE,
            data = MessageEventData(
                messageId = result.messageId,
                roomId = result.chatRoomId,
                senderId = result.senderId,
                content = result.content,
                status = "SENT",
                createdAt = result.createdAt,
            ),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)

        messagingTemplate.convertAndSendToUser(
            principal.name,
            "/queue/chat.ack",
            MessageAck(messageId = result.messageId, status = "SENT"),
        )
        log.debug { "Message sent: roomId=${payload.roomId}, senderId=$accountId, messageId=${result.messageId}" }
    }

    @MessageMapping("/chat.typing.start")
    fun handleTypingStart(payload: TypingPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        verifyJoinedMember(payload.roomId, accountId)
        val event = ChatEvent(
            type = ChatEventType.TYPING_START,
            data = TypingEventData(userId = accountId),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
    }

    @MessageMapping("/chat.typing.stop")
    fun handleTypingStop(payload: TypingPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        verifyJoinedMember(payload.roomId, accountId)
        val event = ChatEvent(
            type = ChatEventType.TYPING_STOP,
            data = TypingEventData(userId = accountId),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
    }

    @MessageMapping("/chat.read")
    fun handleRead(payload: ReadPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        readMessagesUseCase.execute(accountId, payload.roomId)
        val event = ChatEvent(
            type = ChatEventType.READ,
            data = ReadEventData(userId = accountId, readAt = Instant.now()),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
        log.debug { "Messages read: roomId=${payload.roomId}, accountId=$accountId" }
    }

    @MessageExceptionHandler(BusinessException::class)
    @SendToUser("/queue/errors")
    fun handleBusinessException(ex: BusinessException): ChatErrorEvent {
        log.warn { "STOMP handler error: ${ex.errorCode.name} - ${ex.message}" }
        return ChatErrorEvent(code = ex.errorCode.name, message = ex.message ?: ex.errorCode.message)
    }

    @MessageExceptionHandler(Exception::class)
    @SendToUser("/queue/errors")
    fun handleException(ex: Exception): ChatErrorEvent {
        log.error(ex) { "Unexpected STOMP handler error" }
        return ChatErrorEvent(code = ErrorCode.INTERNAL_ERROR.name, message = ErrorCode.INTERNAL_ERROR.message)
    }

    private fun verifyJoinedMember(roomId: Long, accountId: Long) {
        val member = roomMemberStore.findByRoomIdAndAccountId(roomId, accountId)
        if (member == null || !member.isJoined()) {
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }
    }
}
