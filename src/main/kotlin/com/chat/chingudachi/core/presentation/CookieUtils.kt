package com.chat.chingudachi.core.presentation

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import java.time.Duration

fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String, maxAge: Duration) {
    val cookie = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/api/auth/refresh")
        .maxAge(maxAge)
        .build()
    response.addHeader("Set-Cookie", cookie.toString())
}

fun clearRefreshTokenCookie(response: HttpServletResponse) {
    val cookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/api/auth/refresh")
        .maxAge(0)
        .build()
    response.addHeader("Set-Cookie", cookie.toString())
}
