package com.chat.chingudachi.fixture

import com.chat.chingudachi.auth.domain.AuthToken
import java.time.Duration
import java.time.Instant

object AuthTokenFixture {
    fun create(
        id: Long = 0L,
        accountId: Long = 0L,
        refreshToken: String = "test-refresh-token",
        expiresAt: Instant = Instant.now().plus(Duration.ofDays(30)),
    ): AuthToken =
        AuthToken(
            id = id,
            accountId = accountId,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        )
}
