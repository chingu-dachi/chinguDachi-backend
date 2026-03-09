package com.chat.chingudachi.auth.application

import com.chat.chingudachi.auth.application.command.AuthenticateCommand
import com.chat.chingudachi.auth.application.port.AccountCredentialStore
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.auth.application.port.AuthTokenStore
import com.chat.chingudachi.auth.application.port.OAuthClient
import com.chat.chingudachi.auth.application.port.TokenProvider
import com.chat.chingudachi.auth.application.result.AuthenticateResult
import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.AccountType
import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.CredentialType
import com.chat.chingudachi.auth.domain.AuthToken
import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthenticateUseCase(
    private val oAuthClient: OAuthClient,
    private val accountStore: AccountStore,
    private val accountCredentialStore: AccountCredentialStore,
    private val authTokenStore: AuthTokenStore,
    private val tokenProvider: TokenProvider,
) {
    @Transactional
    fun authenticate(command: AuthenticateCommand): AuthenticateResult {
        val oAuthUserInfo = oAuthClient.authenticate(command.code, command.redirectUri)
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
            return accountStore.getById(existing.accountId)
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
                accountId = account.id,
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
                accountId = account.id,
                refreshToken = refreshToken,
                expiresAt = Instant.now().plus(tokenProvider.refreshTokenExpiry),
            ),
        )

        return AuthenticateResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            onboardingRequired = !account.isOnboardingComplete(),
            consentRequired = account.isConsentRequired(),
        )
    }

    private fun toCredentialType(provider: OAuthProvider): CredentialType =
        when (provider) {
            OAuthProvider.GOOGLE -> CredentialType.GOOGLE_OAUTH
        }

}
