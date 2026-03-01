package com.chat.chingudachi.infrastructure.filter

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class MdcLoggingFilter : OncePerRequestFilter() {
    companion object {
        const val REQUEST_ID = "requestId"
        const val METHOD = "method"
        const val URI = "uri"
        const val CLIENT_IP = "clientIp"
        const val USER_ID = "userId"
        const val USER_AGENT = "userAgent"
        const val ORIGIN = "origin"

        private const val REQUEST_ID_LENGTH = 8
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.nanoTime()
        try {
            initializeMdc(request)
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = (System.nanoTime() - startTime) / 1_000_000
            log.info { "${request.method} ${request.requestURI} ${response.status} ${durationMs}ms" }
            MDC.clear()
        }
    }

    private fun initializeMdc(request: HttpServletRequest) {
        MDC.put(REQUEST_ID, generateRequestId())
        MDC.put(METHOD, request.method)
        MDC.put(URI, request.requestURI)
        MDC.put(CLIENT_IP, resolveClientIp(request))
        // TODO: Auth 구현 시 SecurityContextHolder에서 userId 추출로 교체
        MDC.put(USER_ID, "anonymous")
        MDC.put(USER_AGENT, request.getHeader("User-Agent") ?: "")
        MDC.put(ORIGIN, request.getHeader("Origin") ?: "")
    }

    private fun generateRequestId(): String =
        UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH)

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim()
        }
        return request.remoteAddr
    }
}
