package com.chat.chingudachi.domain.account

import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AccountTest : DescribeSpec() {
    init {
        describe("isOnboardingComplete") {
            context("계정 생성 후 ") {
                it("모든 필수 onBoarding 항목을 채우면 완료된 것으로 간주한다.") {
                    val account =
                        AccountFixture.create(
                            nickname = Nickname("test"),
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account.isOnboardingComplete() shouldBe true
                }

                it("nativeLanguage가 없으면 완료되지 않는다.") {
                    val account =
                        AccountFixture.create(
                            nickname = Nickname("test"),
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                        )

                    account.isOnboardingComplete() shouldBe false
                }

                it("nickname이 없으면 완료되지 않는다.") {
                    val account =
                        AccountFixture.create(
                            birthDate = LocalDate.now(),
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account.isOnboardingComplete() shouldBe false
                }

                it("birthDate가 없으면 완료되지 않는다.") {
                    val account =
                        AccountFixture.create(
                            nickname = Nickname("test"),
                            nation = Nation.KR,
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account.isOnboardingComplete() shouldBe false
                }

                it("nation이 없으면 완료되지 않는다.") {
                    val account =
                        AccountFixture.create(
                            nickname = Nickname("test"),
                            birthDate = LocalDate.now(),
                            nativeLanguage = Nation.KR.toNativeLanguage(),
                        )

                    account.isOnboardingComplete() shouldBe false
                }
            }
        }
    }
}
