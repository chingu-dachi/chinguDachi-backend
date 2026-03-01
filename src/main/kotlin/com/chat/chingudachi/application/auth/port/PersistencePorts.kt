package com.chat.chingudachi.application.auth.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.auth.AuthToken

interface AccountStore {
    fun save(account: Account): Account
}

interface AccountCredentialStore {
    fun save(credential: AccountCredential): AccountCredential
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}

interface AuthTokenStore {
    fun save(authToken: AuthToken): AuthToken
    fun deleteByAccountId(accountId: Long)
}
