package com.chat.chingudachi.presentation.chat

import com.chat.chingudachi.application.chat.ReadMessagesUseCase
import com.chat.chingudachi.application.chat.SendMessageUseCase
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.getJoinedMember
import com.chat.chingudachi.domain.common.BusinessException
import com.chat.chingudachi.domain.common.ErrorCode
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
            type = ChatEventType.NEW_MESSAGE,
            data = MessageEventData(
                messageId = result.messageId,
                roomId = result.chatRoomId,
                senderId = result.senderId,
                originalText = result.content,
                translatedText = result.translatedContent,
                status = "SENT",
                createdAt = result.createdAt,
            ),
        )
        broadcast("/topic/chat/${payload.roomId}", event, "roomId=${payload.roomId}, messageId=${result.messageId}")

        sendToUser(
            principal.name,
            "/queue/chat.ack",
            MessageAck(messageId = result.messageId, status = "saved"),
            "messageId=${result.messageId}",
        )
        log.debug { "Message sent: roomId=${payload.roomId}, senderId=$accountId, messageId=${result.messageId}" }
    }

    @MessageMapping("/chat.typing.start")
    fun handleTypingStart(payload: TypingPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        roomMemberStore.getJoinedMember(payload.roomId, accountId)
        val event = ChatEvent(
            type = ChatEventType.TYPING_START,
            data = TypingEventData(userId = accountId),
        )
        broadcast("/topic/chat/${payload.roomId}", event, "roomId=${payload.roomId}, type=TYPING_START")
    }

    @MessageMapping("/chat.typing.stop")
    fun handleTypingStop(payload: TypingPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        roomMemberStore.getJoinedMember(payload.roomId, accountId)
        val event = ChatEvent(
            type = ChatEventType.TYPING_STOP,
            data = TypingEventData(userId = accountId),
        )
        broadcast("/topic/chat/${payload.roomId}", event, "roomId=${payload.roomId}, type=TYPING_STOP")
    }

    @MessageMapping("/chat.read")
    fun handleRead(payload: ReadPayload, principal: Principal) {
        val accountId = principal.name.toLong()
        readMessagesUseCase.execute(accountId, payload.roomId)
        val event = ChatEvent(
            type = ChatEventType.READ,
            data = ReadEventData(userId = accountId, readAt = Instant.now()),
        )
        broadcast("/topic/chat/${payload.roomId}", event, "roomId=${payload.roomId}, type=READ")
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

    private fun broadcast(destination: String, event: ChatEvent, context: String) {
        try {
            messagingTemplate.convertAndSend(destination, event)
        } catch (e: Exception) {
            log.error(e) { "Broadcast failed: destination=$destination, $context" }
        }
    }

    private fun sendToUser(user: String, destination: String, payload: Any, context: String) {
        try {
            messagingTemplate.convertAndSendToUser(user, destination, payload)
        } catch (e: Exception) {
            log.error(e) { "Send to user failed: user=$user, destination=$destination, $context" }
        }
    }
}
