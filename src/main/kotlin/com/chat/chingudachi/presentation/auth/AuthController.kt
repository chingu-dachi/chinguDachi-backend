package com.chat.chingudachi.presentation.auth

import com.chat.chingudachi.application.auth.AuthConstants
import com.chat.chingudachi.application.auth.AuthenticateUseCase
import com.chat.chingudachi.application.auth.RefreshTokenUseCase
import com.chat.chingudachi.application.auth.command.AuthenticateCommand
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticateUseCase: AuthenticateUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {
    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        response: HttpServletResponse,
    ): LoginResponse {
        val result = authenticateUseCase.authenticate(AuthenticateCommand(request.code))
        addRefreshTokenCookie(response, result.refreshToken)
        return LoginResponse(
            accessToken = result.accessToken,
            onboardingRequired = result.onboardingRequired,
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): LoginResponse {
        if (refreshToken == null) {
            throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)
        }
        val result = refreshTokenUseCase.refresh(refreshToken)
        addRefreshTokenCookie(response, result.refreshToken)
        return LoginResponse(
            accessToken = result.accessToken,
            onboardingRequired = result.onboardingRequired,
        )
    }

    private fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/auth/refresh")
            .maxAge(AuthConstants.REFRESH_TOKEN_TTL)
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }
}

data class GoogleLoginRequest(val code: String)

data class LoginResponse(
    val accessToken: String,
    val onboardingRequired: Boolean,
)
