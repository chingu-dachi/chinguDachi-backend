package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.NativeLanguage
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.persistence.EntityManager
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class AccountPersistenceTest(
    private val accountRepository: AccountRepository,
    private val entityManager: EntityManager,
) : DescribeSpec() {
    init {
        describe("Account 엔티티 저장/조회") {
            it("Nickname value class가 정상적으로 매핑된다") {
                val account = AccountFixture.create(
                    id = 0,
                    nickname = Nickname("테스트"),
                    birthDate = LocalDate.of(2000, 1, 1),
                    nation = Nation.KR,
                    nativeLanguage = NativeLanguage.KO,
                )

                val saved = accountRepository.saveAndFlush(account)
                entityManager.clear()

                val found = accountRepository.findById(saved.id).get()
                found.nickname?.value shouldBe "테스트"
                found.nation shouldBe Nation.KR
                found.nativeLanguage shouldBe NativeLanguage.KO
            }

            it("nickname이 null이어도 저장/조회가 정상 동작한다") {
                val account = AccountFixture.create(id = 0)

                val saved = accountRepository.saveAndFlush(account)
                entityManager.clear()

                val found = accountRepository.findById(saved.id).get()
                found.nickname shouldBe null
                found.id shouldNotBe 0
            }
        }
    }
}
