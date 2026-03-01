package com.chat.chingudachi.infrastructure.persistence.auth

import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.AuthTokenFixture
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.persistence.EntityManager
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class AuthTokenRepositoryTest(
    private val authTokenRepository: AuthTokenRepository,
    private val accountRepository: AccountRepository,
    private val entityManager: EntityManager,
) : DescribeSpec() {
    init {
        describe("AuthToken 영속성") {
            it("AuthToken을 저장하고 조회할 수 있다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                val authToken = AuthTokenFixture.create(
                    account = account,
                    refreshToken = "saved-refresh-token",
                )

                val saved = authTokenRepository.saveAndFlush(authToken)
                entityManager.clear()

                val found = authTokenRepository.findById(saved.id).get()
                found.refreshToken shouldBe "saved-refresh-token"
                found.account.id shouldBe account.id
            }
        }

        describe("findByAccountIdAndRefreshToken") {
            it("accountId와 refreshToken으로 조회할 수 있다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                val authToken = authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(
                        account = account,
                        refreshToken = "matching-token",
                    ),
                )
                entityManager.clear()

                val found = authTokenRepository.findByAccountIdAndRefreshToken(
                    accountId = account.id,
                    refreshToken = "matching-token",
                )

                found.shouldNotBeNull()
                found.id shouldBe authToken.id
            }

            it("일치하지 않는 refreshToken이면 null을 반환한다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(
                        account = account,
                        refreshToken = "original-token",
                    ),
                )
                entityManager.clear()

                val found = authTokenRepository.findByAccountIdAndRefreshToken(
                    accountId = account.id,
                    refreshToken = "wrong-token",
                )

                found shouldBe null
            }
        }

        describe("deleteByAccountId") {
            it("해당 accountId의 모든 토큰을 삭제한다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(account = account, refreshToken = "token-1"),
                )
                authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(account = account, refreshToken = "token-2"),
                )
                entityManager.flush()
                entityManager.clear()

                authTokenRepository.deleteByAccountId(account.id)
                entityManager.flush()
                entityManager.clear()

                authTokenRepository.findAll() shouldHaveSize 0
            }
        }

        describe("deleteByExpiresAtBefore") {
            it("만료된 토큰만 삭제한다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(
                        account = account,
                        refreshToken = "expired-token",
                        expiresAt = Instant.now().minus(Duration.ofDays(1)),
                    ),
                )
                authTokenRepository.saveAndFlush(
                    AuthTokenFixture.create(
                        account = account,
                        refreshToken = "valid-token",
                        expiresAt = Instant.now().plus(Duration.ofDays(30)),
                    ),
                )
                entityManager.flush()
                entityManager.clear()

                authTokenRepository.deleteByExpiresAtBefore(Instant.now())
                entityManager.flush()
                entityManager.clear()

                val remaining = authTokenRepository.findAll()
                remaining shouldHaveSize 1
                remaining[0].refreshToken shouldBe "valid-token"
            }
        }
    }
}
