package com.chat.chingudachi.infrastructure.security

import com.chat.chingudachi.application.auth.port.TokenProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            extractToken(request)?.let { token ->
                if (tokenProvider.validateToken(token)) {
                    val accountId = tokenProvider.parseAccountId(token)
                    val authentication = UsernamePasswordAuthenticationToken.authenticated(
                        accountId, null, emptyList(),
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                    MDC.put("userId", accountId.toString())
                } else {
                    log.debug { "JWT validation failed for request: ${request.requestURI}" }
                }
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("userId")
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.substring(BEARER_PREFIX.length) else null
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
