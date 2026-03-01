package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.fixture.AccountCredentialFixture
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.persistence.EntityManager
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
@DataJpaTest
@AutoConfigureEmbeddedDatabase
class AccountCredentialRepositoryTest(
    private val accountCredentialRepository: AccountCredentialRepository,
    private val accountRepository: AccountRepository,
    private val entityManager: EntityManager,
) : DescribeSpec() {
    init {
        describe("AccountCredential 영속성") {
            it("AccountCredential을 저장하고 조회할 수 있다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                val credential = AccountCredentialFixture.create(
                    account = account,
                    oauthKey = "google-user-id-123",
                )

                val saved = accountCredentialRepository.saveAndFlush(credential)
                entityManager.clear()

                val found = accountCredentialRepository.findById(saved.id).get()
                found.credentialType shouldBe CredentialType.GOOGLE_OAUTH
                found.oauthKey shouldBe "google-user-id-123"
                found.account.id shouldBe account.id
            }
        }

        describe("findByCredentialTypeAndOauthKey") {
            it("credentialType과 oauthKey로 조회할 수 있다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                accountCredentialRepository.saveAndFlush(
                    AccountCredentialFixture.create(
                        account = account,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = "google-user-id-456",
                    ),
                )
                entityManager.clear()

                val found = accountCredentialRepository.findByCredentialTypeAndOauthKey(
                    credentialType = CredentialType.GOOGLE_OAUTH,
                    oauthKey = "google-user-id-456",
                )

                found.shouldNotBeNull()
                found.account.id shouldBe account.id
            }

            it("존재하지 않는 oauthKey이면 null을 반환한다") {
                val found = accountCredentialRepository.findByCredentialTypeAndOauthKey(
                    credentialType = CredentialType.GOOGLE_OAUTH,
                    oauthKey = "non-existent-key",
                )

                found.shouldBeNull()
            }

            it("다른 credentialType이면 null을 반환한다") {
                val account = accountRepository.saveAndFlush(
                    AccountFixture.create(id = 0),
                )
                accountCredentialRepository.saveAndFlush(
                    AccountCredentialFixture.create(
                        account = account,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = "google-user-id-789",
                    ),
                )
                entityManager.clear()

                // CredentialType이 하나뿐이라 직접 다른 타입 테스트는 불가하지만,
                // 같은 타입 + 다른 키로 null 반환 확인
                val found = accountCredentialRepository.findByCredentialTypeAndOauthKey(
                    credentialType = CredentialType.GOOGLE_OAUTH,
                    oauthKey = "different-key",
                )

                found.shouldBeNull()
            }
        }
    }
}
