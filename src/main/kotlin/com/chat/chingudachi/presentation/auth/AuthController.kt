package com.chat.chingudachi.presentation.auth

import com.chat.chingudachi.application.auth.AuthenticateUseCase
import com.chat.chingudachi.application.auth.LogoutUseCase
import com.chat.chingudachi.application.auth.RefreshTokenUseCase
import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.application.auth.command.AuthenticateCommand
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import com.chat.chingudachi.presentation.common.AuthAccountId
import com.chat.chingudachi.presentation.common.addRefreshTokenCookie
import com.chat.chingudachi.presentation.common.clearRefreshTokenCookie
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
