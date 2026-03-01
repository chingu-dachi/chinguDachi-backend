package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.command.AuthenticateCommand
import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.application.auth.result.AuthenticateResult
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.AccountType
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.auth.AuthToken
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import com.chat.chingudachi.infrastructure.jwt.JwtProvider
import com.chat.chingudachi.infrastructure.persistence.account.AccountCredentialRepository
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import com.chat.chingudachi.infrastructure.persistence.auth.AuthTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
@Transactional
class AuthenticateUseCase(
    private val oAuthClient: OAuthClient,
    private val accountRepository: AccountRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val jwtProvider: JwtProvider,
) {
    fun authenticate(command: AuthenticateCommand): AuthenticateResult {
        val oAuthUserInfo = oAuthClient.authenticate(command.code)
        val account = findOrCreateAccount(oAuthUserInfo)

        val accessToken = jwtProvider.createAccessToken(account.id)
        val refreshToken = jwtProvider.createRefreshToken(account.id)

        authTokenRepository.deleteByAccountId(account.id)
        authTokenRepository.save(
            AuthToken(
                account = account,
                refreshToken = refreshToken,
                expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL),
            ),
        )

        return AuthenticateResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            onboardingRequired = !account.isOnboardingComplete(),
        )
    }

    private fun findOrCreateAccount(oAuthUserInfo: OAuthUserInfo): Account {
        val credentialType = toCredentialType(oAuthUserInfo.provider)
        val existing = accountCredentialRepository.findByCredentialTypeAndOauthKey(
            credentialType = credentialType,
            oauthKey = oAuthUserInfo.providerUserId,
        )

        if (existing != null) {
            return existing.account
        }

        val account = accountRepository.save(
            Account(
                accountType = AccountType.USER,
                accountStatus = AccountStatus.NOT_CONSENT,
                email = oAuthUserInfo.email,
            ),
        )

        accountCredentialRepository.save(
            AccountCredential(
                account = account,
                credentialType = credentialType,
                oauthKey = oAuthUserInfo.providerUserId,
            ),
        )

        return account
    }

    private fun toCredentialType(provider: OAuthProvider): CredentialType =
        when (provider) {
            OAuthProvider.GOOGLE -> CredentialType.GOOGLE_OAUTH
        }

    companion object {
        private val REFRESH_TOKEN_TTL = Duration.ofDays(30)
    }
}
