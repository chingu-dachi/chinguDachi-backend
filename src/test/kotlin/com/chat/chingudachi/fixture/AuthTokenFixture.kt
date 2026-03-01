package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.auth.AuthToken
import java.time.Duration
import java.time.Instant

object AuthTokenFixture {
    fun create(
        id: Long = 0L,
        account: Account = AccountFixture.create(id = 0L),
        refreshToken: String = "test-refresh-token",
        expiresAt: Instant = Instant.now().plus(Duration.ofDays(30)),
    ): AuthToken =
        AuthToken(
            id = id,
            account = account,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        )
}
