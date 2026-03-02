package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import com.chat.chingudachi.domain.interest.InterestTag
import com.chat.chingudachi.domain.interest.UserInterest
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class GetUserProfileUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>()
    private val userInterestStore = mockk<UserInterestStore>()
    private val useCase = GetUserProfileUseCase(accountStore, userInterestStore)

    init {
        afterEach { clearMocks(accountStore, userInterestStore) }

        describe("execute") {
            val userId = 1L

            context("ACTIVE 유저") {
                it("UserPublicProfile을 반환한다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val tag = InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1)
                    val interest = UserInterest(account = account, interestTag = tag)

                    every { accountStore.getById(userId) } returns account
                    every { userInterestStore.findByAccountId(userId) } returns listOf(interest)

                    val result = useCase.execute(userId)

                    result.userId shouldBe userId
                    result.nickname shouldBe "테스트유저"
                    result.nationality shouldBe "KR"
                    result.translateLanguage shouldBe "ja"
                    result.birthYear shouldBe 1995
                    result.interests shouldBe listOf("TRAVEL")
                }
            }

            context("온보딩 미완료 유저") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.NOT_CONSENT,
                    )
                    every { accountStore.getById(userId) } returns account

                    shouldThrow<NotFoundException> {
                        useCase.execute(userId)
                    }
                }
            }

            context("INACTIVE 유저") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.INACTIVE,
                    )
                    every { accountStore.getById(userId) } returns account

                    shouldThrow<NotFoundException> {
                        useCase.execute(userId)
                    }
                }
            }

            context("SUSPENDED 유저") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.SUSPENDED,
                    )
                    every { accountStore.getById(userId) } returns account

                    shouldThrow<NotFoundException> {
                        useCase.execute(userId)
                    }
                }
            }

            context("존재하지 않는 유저") {
                it("NotFoundException을 던진다") {
                    every { accountStore.getById(userId) } throws NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

                    shouldThrow<NotFoundException> {
                        useCase.execute(userId)
                    }
                }
            }
        }
    }
}
