package com.chat.chingudachi.domain.account

import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AccountTest : DescribeSpec() {
    init {
        describe("isOnboardingComplete") {
            context("온보딩이 완료된 상태") {
                it("ACTIVE 상태이면 true를 반환한다") {
                    val account = AccountFixture.create(accountStatus = AccountStatus.ACTIVE)

                    account.isOnboardingComplete() shouldBe true
                }

                it("INACTIVE 상태이면 true를 반환한다") {
                    val account = AccountFixture.create(accountStatus = AccountStatus.INACTIVE)

                    account.isOnboardingComplete() shouldBe true
                }

                it("SUSPENDED 상태이면 true를 반환한다") {
                    val account = AccountFixture.create(accountStatus = AccountStatus.SUSPENDED)

                    account.isOnboardingComplete() shouldBe true
                }
            }

            context("온보딩이 완료되지 않은 상태") {
                it("NOT_CONSENT 상태이면 false를 반환한다") {
                    val account = AccountFixture.create(accountStatus = AccountStatus.NOT_CONSENT)

                    account.isOnboardingComplete() shouldBe false
                }

                it("NOT_ENOUGH_DETAIL 상태이면 false를 반환한다") {
                    val account = AccountFixture.create(accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)

                    account.isOnboardingComplete() shouldBe false
                }
            }
        }

        describe("completeOnboarding") {
            it("상태를 ACTIVE로 변경하고 프로필 필드를 설정한다") {
                val account = AccountFixture.create()
                val nickname = Nickname("테스트유저")
                val birthDate = LocalDate.of(2000, 1, 1)
                val nation = Nation.KR

                account.completeOnboarding(nickname, birthDate, nation)

                account.accountStatus shouldBe AccountStatus.ACTIVE
                account.nickname shouldBe nickname
                account.birthDate shouldBe birthDate
                account.nation shouldBe nation
                account.nativeLanguage shouldBe NativeLanguage.KO
                account.isOnboardingComplete() shouldBe true
            }
        }
    }
}
