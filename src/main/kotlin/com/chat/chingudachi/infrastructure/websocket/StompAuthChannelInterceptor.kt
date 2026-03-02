package com.chat.chingudachi.infrastructure.websocket

import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import com.chat.chingudachi.domain.common.UnauthorizedException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import java.security.Principal

private val log = KotlinLogging.logger {}

@Component
class StompAuthChannelInterceptor(
    private val tokenProvider: TokenProvider,
    private val roomMemberStore: RoomMemberStore,
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        when (accessor.command) {
            StompCommand.CONNECT -> handleConnect(accessor)
            StompCommand.SEND, StompCommand.SUBSCRIBE -> requireAuthenticated(accessor)
            else -> {}
        }

        if (accessor.command == StompCommand.SUBSCRIBE) {
            handleSubscribe(accessor)
        }

        return message
    }

    private fun handleConnect(accessor: StompHeaderAccessor) {
        val token = extractToken(accessor)
        val accountId = tokenProvider.parseAccountId(token)
        accessor.user = StompPrincipal(accountId)
        log.debug { "STOMP CONNECT: accountId=$accountId" }
    }

    private fun requireAuthenticated(accessor: StompHeaderAccessor) {
        if (accessor.user == null) {
            log.warn { "STOMP ${accessor.command}: unauthenticated request rejected" }
            throw UnauthorizedException(ErrorCode.UNAUTHORIZED)
        }
    }

    private fun handleSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return
        val roomId = extractChatRoomId(destination) ?: return
        val accountId = (accessor.user as? StompPrincipal)?.accountId
            ?: throw UnauthorizedException(ErrorCode.UNAUTHORIZED)

        val member = roomMemberStore.findByRoomIdAndAccountId(roomId, accountId)
        if (member == null || !member.isJoined()) {
            log.warn { "STOMP SUBSCRIBE denied: accountId=$accountId, roomId=$roomId" }
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }
        log.debug { "STOMP SUBSCRIBE: accountId=$accountId, roomId=$roomId" }
    }

    private fun extractToken(accessor: StompHeaderAccessor): String {
        val authHeader = accessor.getFirstNativeHeader("Authorization")
            ?: throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)
        if (!authHeader.startsWith("Bearer ")) {
            throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)
        }
        return authHeader.substring(7)
    }

    private fun extractChatRoomId(destination: String): Long? {
        val prefix = "/topic/chat/"
        if (!destination.startsWith(prefix)) return null
        return destination.substringAfter(prefix).toLongOrNull()
    }
}

data class StompPrincipal(val accountId: Long) : Principal {
    override fun getName(): String = accountId.toString()
}
