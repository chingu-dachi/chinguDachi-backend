package com.chat.chingudachi.auth.presentation

import com.chat.chingudachi.auth.application.AuthenticateUseCase
import com.chat.chingudachi.auth.application.LogoutUseCase
import com.chat.chingudachi.auth.application.RefreshTokenUseCase
import com.chat.chingudachi.auth.application.port.TokenProvider
import com.chat.chingudachi.auth.application.command.AuthenticateCommand
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
    private val authenticateUseCase: AuthenticateUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val tokenProvider: TokenProvider,
) {
    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        response: HttpServletResponse,
    ): LoginResponse {
        val result = authenticateUseCase.authenticate(AuthenticateCommand(request.code, request.redirectUri))
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
        logoutUseCase.execute(accountId)
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
        val result = refreshTokenUseCase.refresh(refreshToken)
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
