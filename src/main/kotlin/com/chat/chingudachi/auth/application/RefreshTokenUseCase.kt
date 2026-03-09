package com.chat.chingudachi.auth.application

import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.auth.application.port.AuthTokenStore
import com.chat.chingudachi.auth.application.port.TokenProvider
import com.chat.chingudachi.auth.application.result.AuthenticateResult
import com.chat.chingudachi.auth.domain.AuthToken
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefreshTokenUseCase(
    private val accountStore: AccountStore,
    private val authTokenStore: AuthTokenStore,
    private val tokenProvider: TokenProvider,
) {
    @Transactional
    fun refresh(refreshToken: String): AuthenticateResult {
        val accountId = tokenProvider.parseAccountId(refreshToken)

        val authToken = authTokenStore.findByAccountIdAndRefreshToken(accountId, refreshToken)
            ?: throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)

        if (authToken.isExpired()) {
            authTokenStore.deleteByAccountId(accountId)
            throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED)
        }

        val account = accountStore.getById(accountId)

        authTokenStore.deleteByAccountId(accountId)

        val newAccessToken = tokenProvider.createAccessToken(accountId)
        val newRefreshToken = tokenProvider.createRefreshToken(accountId)

        authTokenStore.save(
            AuthToken(
                accountId = accountId,
                refreshToken = newRefreshToken,
                expiresAt = Instant.now().plus(tokenProvider.refreshTokenExpiry),
            ),
        )

        return AuthenticateResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            onboardingRequired = !account.isOnboardingComplete(),
            consentRequired = account.isConsentRequired(),
        )
    }

}
