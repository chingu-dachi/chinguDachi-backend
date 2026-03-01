package com.chat.chingudachi.filter

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
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.nanoTime()
        try {
            setMdc(request)
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = (System.nanoTime() - startTime) / 1_000_000
            log.info { "${request.method} ${request.requestURI} ${response.status} ${durationMs}ms" }
            MDC.clear()
        }
    }

    private fun setMdc(request: HttpServletRequest) {
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8))
        MDC.put(METHOD, request.method)
        MDC.put(URI, request.requestURI)
        MDC.put(CLIENT_IP, resolveClientIp(request))
        MDC.put(USER_ID, "anonymous")
        MDC.put(USER_AGENT, request.getHeader("User-Agent") ?: "")
        MDC.put(ORIGIN, request.getHeader("Origin") ?: "")
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim()
        }
        return request.remoteAddr
    }
}
