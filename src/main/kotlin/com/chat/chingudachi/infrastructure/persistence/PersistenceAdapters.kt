package com.chat.chingudachi.infrastructure.persistence

import com.chat.chingudachi.application.auth.port.AccountCredentialStore
import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.auth.AuthToken
import com.chat.chingudachi.infrastructure.persistence.account.AccountCredentialRepository
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import com.chat.chingudachi.infrastructure.persistence.auth.AuthTokenRepository
import org.springframework.stereotype.Repository

@Repository
class AccountStoreAdapter(
    private val accountRepository: AccountRepository,
) : AccountStore {
    override fun save(account: Account): Account = accountRepository.save(account)
}

@Repository
class AccountCredentialStoreAdapter(
    private val accountCredentialRepository: AccountCredentialRepository,
) : AccountCredentialStore {
    override fun save(credential: AccountCredential): AccountCredential =
        accountCredentialRepository.save(credential)

    override fun findByCredentialTypeAndOauthKey(
        credentialType: CredentialType,
        oauthKey: String,
    ): AccountCredential? =
        accountCredentialRepository.findByCredentialTypeAndOauthKey(credentialType, oauthKey)
}

@Repository
class AuthTokenStoreAdapter(
    private val authTokenRepository: AuthTokenRepository,
) : AuthTokenStore {
    override fun save(authToken: AuthToken): AuthToken = authTokenRepository.save(authToken)
    override fun deleteByAccountId(accountId: Long) = authTokenRepository.deleteByAccountId(accountId)
}
