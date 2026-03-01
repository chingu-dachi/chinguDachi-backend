package com.chat.chingudachi.application.auth.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.auth.AuthToken
import com.chat.chingudachi.domain.interest.UserInterest

interface AccountStore {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
}

interface AccountCredentialStore {
    fun save(credential: AccountCredential): AccountCredential
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}

interface UserInterestStore {
    fun findByAccountId(accountId: Long): List<UserInterest>
}

interface AuthTokenStore {
    fun save(authToken: AuthToken): AuthToken
    fun findByAccountIdAndRefreshToken(accountId: Long, refreshToken: String): AuthToken?
    fun deleteByAccountId(accountId: Long)
}
