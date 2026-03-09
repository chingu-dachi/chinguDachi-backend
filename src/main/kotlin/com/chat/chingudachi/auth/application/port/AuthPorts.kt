package com.chat.chingudachi.auth.application.port

import com.chat.chingudachi.account.domain.AccountCredential
import com.chat.chingudachi.account.domain.CredentialType
import com.chat.chingudachi.auth.domain.AuthToken

interface AccountCredentialStore {
    fun save(credential: AccountCredential): AccountCredential
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}

interface AuthTokenStore {
    fun save(authToken: AuthToken): AuthToken
    fun findByAccountIdAndRefreshToken(accountId: Long, refreshToken: String): AuthToken?
    fun deleteByAccountId(accountId: Long)
}
