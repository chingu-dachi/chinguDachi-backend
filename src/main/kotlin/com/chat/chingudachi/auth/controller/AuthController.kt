package com.chat.chingudachi.auth.controller

import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.auth.service.AuthService
import com.chat.chingudachi.auth.service.AuthenticateCommand
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.UnauthorizedException
import com.chat.chingudachi.core.presentation.AuthAccountId
import com.chat.chingudachi.core.presentation.addRefreshTokenCookie
import com.chat.chingudachi.core.presentation.clearRefreshTokenCookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenProvider: TokenProvider,
) {
    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        response: HttpServletResponse,
    ): LoginResponse {
        val result = authService.authenticate(AuthenticateCommand(request.code, request.redirectUri))
        addRefreshTokenCookie(response, result.refreshToken, tokenProvider.refreshTokenExpiry)
        return LoginResponse(
            accessToken = result.accessToken,
            onboardingRequired = result.onboardingRequired,
            consentRequired = result.consentRequired,
        )
    }

    @PostMapping("/logout")
    fun logout(
        @AuthAccountId accountId: Long,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> {
        authService.logout(accountId)
        clearRefreshTokenCookie(response)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): LoginResponse {
        if (refreshToken == null) {
            throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)
        }
        val result = authService.refresh(refreshToken)
        addRefreshTokenCookie(response, result.refreshToken, tokenProvider.refreshTokenExpiry)
        return LoginResponse(
            accessToken = result.accessToken,
            onboardingRequired = result.onboardingRequired,
            consentRequired = result.consentRequired,
        )
    }
}

data class GoogleLoginRequest(
    val code: String,
    val redirectUri: String? = null,
)

data class LoginResponse(
    val accessToken: String,
    val onboardingRequired: Boolean,
    val consentRequired: Boolean,
)
