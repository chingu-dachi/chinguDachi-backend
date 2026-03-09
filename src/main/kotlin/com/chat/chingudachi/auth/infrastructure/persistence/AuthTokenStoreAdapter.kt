package com.chat.chingudachi.auth.infrastructure.persistence

import com.chat.chingudachi.auth.application.port.AuthTokenStore
import com.chat.chingudachi.auth.domain.AuthToken
import org.springframework.stereotype.Repository

@Repository
class AuthTokenStoreAdapter(
    private val authTokenRepository: AuthTokenRepository,
) : AuthTokenStore {
    override fun save(authToken: AuthToken): AuthToken = authTokenRepository.save(authToken)

    override fun findByAccountIdAndRefreshToken(accountId: Long, refreshToken: String): AuthToken? =
        authTokenRepository.findByAccountIdAndRefreshToken(accountId, refreshToken)

    override fun deleteByAccountId(accountId: Long) = authTokenRepository.deleteByAccountId(accountId)
}
