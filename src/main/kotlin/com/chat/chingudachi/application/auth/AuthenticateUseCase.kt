package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.command.AuthenticateCommand
import com.chat.chingudachi.application.auth.port.AccountCredentialStore
import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.application.auth.result.AuthenticateResult
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.AccountType
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.auth.AuthToken
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
@Transactional
class AuthenticateUseCase(
    private val oAuthClient: OAuthClient,
    private val accountStore: AccountStore,
    private val accountCredentialStore: AccountCredentialStore,
    private val authTokenStore: AuthTokenStore,
    private val tokenProvider: TokenProvider,
) {
    fun authenticate(command: AuthenticateCommand): AuthenticateResult {
        val oAuthUserInfo = oAuthClient.authenticate(command.code)
        val account = findOrCreateAccount(oAuthUserInfo)
        return issueTokens(account)
    }

    private fun findOrCreateAccount(oAuthUserInfo: OAuthUserInfo): Account {
        val credentialType = toCredentialType(oAuthUserInfo.provider)
        val existing = accountCredentialStore.findByCredentialTypeAndOauthKey(
            credentialType = credentialType,
            oauthKey = oAuthUserInfo.providerUserId,
        )

        if (existing != null) {
            return existing.account
        }

        val account = accountStore.save(
            Account(
                accountType = AccountType.USER,
                accountStatus = AccountStatus.NOT_CONSENT,
                email = oAuthUserInfo.email,
            ),
        )

        accountCredentialStore.save(
            AccountCredential(
                account = account,
                credentialType = credentialType,
                oauthKey = oAuthUserInfo.providerUserId,
            ),
        )

        return account
    }

    private fun issueTokens(account: Account): AuthenticateResult {
        val accessToken = tokenProvider.createAccessToken(account.id)
        val refreshToken = tokenProvider.createRefreshToken(account.id)

        authTokenStore.deleteByAccountId(account.id)
        authTokenStore.save(
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

    private fun toCredentialType(provider: OAuthProvider): CredentialType =
        when (provider) {
            OAuthProvider.GOOGLE -> CredentialType.GOOGLE_OAUTH
        }

    companion object {
        private val REFRESH_TOKEN_TTL = Duration.ofDays(30)
    }
}
