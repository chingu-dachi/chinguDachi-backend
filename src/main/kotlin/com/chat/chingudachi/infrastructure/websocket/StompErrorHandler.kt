package com.chat.chingudachi.infrastructure.websocket

import com.chat.chingudachi.domain.common.BusinessException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler

private val log = KotlinLogging.logger {}

@Component
class StompErrorHandler : StompSubProtocolErrorHandler() {
    override fun handleClientMessageProcessingError(
        clientMessage: Message<ByteArray>?,
        ex: Throwable,
    ): Message<ByteArray>? {
        val cause = ex.cause ?: ex

        val errorMessage = when (cause) {
            is BusinessException -> cause.errorCode.message
            else -> "Internal server error"
        }

        log.warn { "STOMP error: ${cause.javaClass.simpleName} - ${cause.message}" }

        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = errorMessage
        accessor.setLeaveMutable(true)

        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
