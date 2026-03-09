package com.chat.chingudachi.account.domain

import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
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

        describe("isActive") {
            it("ACTIVE 상태이면 true를 반환한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.ACTIVE)

                account.isActive() shouldBe true
            }

            it("INACTIVE 상태이면 false를 반환한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.INACTIVE)

                account.isActive() shouldBe false
            }

            it("SUSPENDED 상태이면 false를 반환한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.SUSPENDED)

                account.isActive() shouldBe false
            }

            it("NOT_CONSENT 상태이면 false를 반환한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.NOT_CONSENT)

                account.isActive() shouldBe false
            }

            it("NOT_ENOUGH_DETAIL 상태이면 false를 반환한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)

                account.isActive() shouldBe false
            }
        }

        describe("completeOnboarding") {
            it("상태를 ACTIVE로 변경하고 프로필 필드를 설정한다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                val nickname = Nickname("테스트유저")
                val birthYear = 2000
                val nation = Nation.KR

                account.completeOnboarding(nickname, birthYear, nation, "자기소개", "서울", null)

                account.accountStatus shouldBe AccountStatus.ACTIVE
                account.nickname shouldBe nickname
                account.birthDate shouldBe LocalDate.of(2000, 1, 1)
                account.nation shouldBe nation
                account.translateLanguage shouldBe TranslateLanguage.JA
                account.bio shouldBe "자기소개"
                account.city shouldBe "서울"
                account.isOnboardingComplete() shouldBe true
            }
        }

        describe("updateProfile") {
            it("전달된 필드만 업데이트한다") {
                val account = AccountFixture.create(
                    accountStatus = AccountStatus.ACTIVE,
                    nickname = Nickname("기존닉네임"),
                )

                account.updateProfile(bio = "새 자기소개", city = "도쿄")

                account.bio shouldBe "새 자기소개"
                account.city shouldBe "도쿄"
                account.nickname shouldBe Nickname("기존닉네임")
                account.profileImageUrl shouldBe null
            }

            it("null 필드는 기존 값을 유지한다") {
                val account = AccountFixture.create(
                    accountStatus = AccountStatus.ACTIVE,
                    nickname = Nickname("기존닉네임"),
                )
                account.bio = "기존 소개"
                account.city = "서울"

                account.updateProfile(nickname = Nickname("새닉네임"))

                account.nickname shouldBe Nickname("새닉네임")
                account.bio shouldBe "기존 소개"
                account.city shouldBe "서울"
            }

            it("모든 필드를 한 번에 업데이트할 수 있다") {
                val account = AccountFixture.create(
                    accountStatus = AccountStatus.ACTIVE,
                    nickname = Nickname("기존닉네임"),
                )

                account.updateProfile(
                    nickname = Nickname("새닉네임"),
                    bio = "새 소개",
                    city = "오사카",
                    profileImageUrl = "https://example.com/img.jpg",
                )

                account.nickname shouldBe Nickname("새닉네임")
                account.bio shouldBe "새 소개"
                account.city shouldBe "오사카"
                account.profileImageUrl shouldBe "https://example.com/img.jpg"
            }
        }

        describe("softDelete") {
            it("deletedAt을 설정하고 상태를 INACTIVE로 변경한다") {
                val account = AccountFixture.create(
                    accountStatus = AccountStatus.ACTIVE,
                    nickname = Nickname("테스트유저"),
                )

                account.softDelete()

                account.deletedAt shouldNotBe null
                account.accountStatus shouldBe AccountStatus.INACTIVE
            }

            it("닉네임을 null로 설정한다") {
                val account = AccountFixture.create(
                    accountStatus = AccountStatus.ACTIVE,
                    nickname = Nickname("테스트유저"),
                )

                account.softDelete()

                account.nickname shouldBe null
            }

            it("이미 삭제된 계정이면 ConflictException을 던진다") {
                val account = AccountFixture.create(accountStatus = AccountStatus.INACTIVE)
                account.deletedAt = Instant.now()

                shouldThrow<ConflictException> {
                    account.softDelete()
                }
            }
        }
    }
}
