package com.chat.chingudachi.presentation.chat

import com.chat.chingudachi.application.chat.ReadMessagesUseCase
import com.chat.chingudachi.application.chat.SendMessageUseCase
import com.chat.chingudachi.infrastructure.websocket.StompPrincipal
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
import java.time.Instant

@Controller
class ChatMessageHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val readMessagesUseCase: ReadMessagesUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    @MessageMapping("/chat.send")
    fun handleSend(payload: SendMessagePayload, principal: Principal) {
        val accountId = (principal as StompPrincipal).accountId
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
    }

    @MessageMapping("/chat.typing.start")
    fun handleTypingStart(payload: TypingPayload, principal: Principal) {
        val accountId = (principal as StompPrincipal).accountId
        val event = ChatEvent(
            type = ChatEventType.TYPING_START,
            data = TypingEventData(userId = accountId),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
    }

    @MessageMapping("/chat.typing.stop")
    fun handleTypingStop(payload: TypingPayload, principal: Principal) {
        val accountId = (principal as StompPrincipal).accountId
        val event = ChatEvent(
            type = ChatEventType.TYPING_STOP,
            data = TypingEventData(userId = accountId),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
    }

    @MessageMapping("/chat.read")
    fun handleRead(payload: ReadPayload, principal: Principal) {
        val accountId = (principal as StompPrincipal).accountId
        readMessagesUseCase.execute(accountId, payload.roomId)
        val event = ChatEvent(
            type = ChatEventType.READ,
            data = ReadEventData(userId = accountId, readAt = Instant.now()),
        )
        messagingTemplate.convertAndSend("/topic/chat/${payload.roomId}", event)
    }
}
