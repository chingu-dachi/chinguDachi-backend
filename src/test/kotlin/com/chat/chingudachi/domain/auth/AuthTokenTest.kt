package com.chat.chingudachi.domain.auth

import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

class AuthTokenTest : DescribeSpec() {
    init {
        describe("AuthToken의") {
            context("isExpired") {
                it("만료 시점이 현재보다 과거이면 true를 반환한다") {
                    val token =
                        AuthToken(
                            account = AccountFixture.create(),
                            refreshToken = "test-refresh-token",
                            expiresAt = Instant.now().minus(Duration.ofHours(1)),
                        )

                    token.isExpired() shouldBe true
                }

                it("만료 시점이 현재보다 미래이면 false를 반환한다") {
                    val token =
                        AuthToken(
                            account = AccountFixture.create(),
                            refreshToken = "test-refresh-token",
                            expiresAt = Instant.now().plus(Duration.ofHours(1)),
                        )

                    token.isExpired() shouldBe false
                }
            }
        }
    }
}
