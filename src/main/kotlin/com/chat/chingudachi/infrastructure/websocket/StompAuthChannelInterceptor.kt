package com.chat.chingudachi.infrastructure.websocket

import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import java.security.Principal

@Component
class StompAuthChannelInterceptor(
    private val tokenProvider: TokenProvider,
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        if (accessor?.command == StompCommand.CONNECT) {
            val token = extractToken(accessor)
            val accountId = tokenProvider.parseAccountId(token)
            accessor.user = StompPrincipal(accountId)
        }
        return message
    }

    private fun extractToken(accessor: StompHeaderAccessor): String {
        val authHeader = accessor.getFirstNativeHeader("Authorization")
            ?: throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)
        if (!authHeader.startsWith("Bearer ")) {
            throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)
        }
        return authHeader.substring(7)
    }
}

data class StompPrincipal(val accountId: Long) : Principal {
    override fun getName(): String = accountId.toString()
}
