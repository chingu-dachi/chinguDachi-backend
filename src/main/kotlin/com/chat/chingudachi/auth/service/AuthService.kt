package com.chat.chingudachi.auth.service

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.AccountType
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.AuthToken
import com.chat.chingudachi.auth.domain.CredentialType
import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo
import com.chat.chingudachi.auth.infra.OAuthClient
import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.auth.repository.AccountCredentialRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.core.domain.UnauthorizedException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class AuthService(
    private val oAuthClient: OAuthClient,
    private val accountRepository: AccountRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val tokenProvider: TokenProvider,
) {
    @Transactional
    fun authenticate(command: AuthenticateCommand): AuthenticateResult {
        val oAuthUserInfo = oAuthClient.authenticate(command.code, command.redirectUri)
        val account = findOrCreateAccount(oAuthUserInfo)
        return issueTokens(account)
    }

    @Transactional
    fun refresh(refreshToken: String): AuthenticateResult {
        val accountId = tokenProvider.parseAccountId(refreshToken)

        val authToken = authTokenRepository.findByAccountIdAndRefreshToken(accountId, refreshToken)
            ?: throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)

        if (authToken.isExpired()) {
            authTokenRepository.deleteByAccountId(accountId)
            throw UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED)
        }

        val account = accountRepository.findByIdOrNull(accountId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

        authTokenRepository.deleteByAccountId(accountId)

        val newAccessToken = tokenProvider.createAccessToken(accountId)
        val newRefreshToken = tokenProvider.createRefreshToken(accountId)

        authTokenRepository.save(
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

    @Transactional
    fun logout(accountId: Long) {
        authTokenRepository.deleteByAccountId(accountId)
        log.debug { "Logout: accountId=$accountId" }
    }

    private fun findOrCreateAccount(oAuthUserInfo: OAuthUserInfo): Account {
        val credentialType = toCredentialType(oAuthUserInfo.provider)
        val existing = accountCredentialRepository.findByCredentialTypeAndOauthKey(
            credentialType = credentialType,
            oauthKey = oAuthUserInfo.providerUserId,
        )

        if (existing != null) {
            return accountRepository.findByIdOrNull(existing.accountId)
                ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
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

        authTokenRepository.deleteByAccountId(account.id)
        authTokenRepository.save(
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

data class AuthenticateCommand(
    val code: String,
    val redirectUri: String? = null,
)

data class AuthenticateResult(
    val accessToken: String,
    val refreshToken: String,
    val onboardingRequired: Boolean,
    val consentRequired: Boolean,
)
