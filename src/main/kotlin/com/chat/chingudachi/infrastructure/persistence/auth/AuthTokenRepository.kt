package com.chat.chingudachi.infrastructure.persistence.auth

import com.chat.chingudachi.domain.auth.AuthToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import java.time.Instant

interface AuthTokenRepository : JpaRepository<AuthToken, Long> {
    fun findByAccountIdAndRefreshToken(accountId: Long, refreshToken: String): AuthToken?

    @Modifying
    fun deleteByAccountId(accountId: Long)

    @Modifying
    fun deleteByExpiresAtBefore(now: Instant)
}
