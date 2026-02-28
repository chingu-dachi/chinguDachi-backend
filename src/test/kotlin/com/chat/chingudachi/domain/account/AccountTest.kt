package com.chat.chingudachi.domain.account

import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AccountTest : DescribeSpec() {
    init {
        describe("isOnboardingComplete") {
            context("계정 생성 후 ") {
                it("모든 필수 onBoarding 항목을 채우면 onBoarding이 완료된 것으로 간주한다.") {
                    val account =
                        AccountFixture.create(
                            nickname = "test",
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account.isOnboardingComplete() shouldBe true
                }
                it("하나라도 마무리되지 않은 항목이 있으면 완료되지 않은 것으로 간주한다..") {
                    val account1 =
                        AccountFixture.create(
                            nickname = "test",
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                        )

                    val account2 =
                        AccountFixture.create(
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    val account3 =
                        AccountFixture.create(
                            nickname = "test",
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    val account4 =
                        AccountFixture.create(
                            nickname = "test",
                            birthDate = LocalDate.now(),
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account1.isOnboardingComplete() shouldBe false
                    account2.isOnboardingComplete() shouldBe false
                    account3.isOnboardingComplete() shouldBe false
                    account4.isOnboardingComplete() shouldBe false
                }
            }
        }
    }
}
