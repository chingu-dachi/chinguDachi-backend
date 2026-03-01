package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.application.auth.result.AuthenticateResult
import com.chat.chingudachi.domain.auth.AuthToken
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
@Transactional
class RefreshTokenUseCase(
    private val authTokenStore: AuthTokenStore,
    private val tokenProvider: TokenProvider,
) {
    fun refresh(refreshToken: String): AuthenticateResult {
        val accountId = tokenProvider.parseAccountId(refreshToken)

        val authToken = authTokenStore.findByAccountIdAndRefreshToken(accountId, refreshToken)
            ?: throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)

        if (authToken.isExpired()) {
            authTokenStore.deleteByAccountId(accountId)
            throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED)
        }

        val account = authToken.account
        val onboardingRequired = !account.isOnboardingComplete()

        authTokenStore.deleteByAccountId(accountId)

        val newAccessToken = tokenProvider.createAccessToken(accountId)
        val newRefreshToken = tokenProvider.createRefreshToken(accountId)

        authTokenStore.save(
            AuthToken(
                account = account,
                refreshToken = newRefreshToken,
                expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL),
            ),
        )

        return AuthenticateResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            onboardingRequired = onboardingRequired,
        )
    }

    companion object {
        private val REFRESH_TOKEN_TTL = Duration.ofDays(30)
    }
}
