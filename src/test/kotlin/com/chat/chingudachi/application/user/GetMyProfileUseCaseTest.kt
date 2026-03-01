package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.NativeLanguage
import com.chat.chingudachi.domain.account.Nickname
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

class GetMyProfileUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>()
    private val userInterestStore = mockk<UserInterestStore>()
    private val useCase = GetMyProfileUseCase(accountStore, userInterestStore)

    init {
        afterEach { clearMocks(accountStore, userInterestStore) }

        describe("execute") {
            val accountId = 1L

            context("온보딩 미완료 유저") {
                it("기본 프로필과 빈 관심사를 반환한다") {
                    val account = AccountFixture.create(id = accountId)
                    every { accountStore.findById(accountId) } returns account
                    every { userInterestStore.findByAccountId(accountId) } returns emptyList()

                    val result = useCase.execute(accountId)

                    result.account.id shouldBe accountId
                    result.account.accountStatus shouldBe AccountStatus.NOT_CONSENT
                    result.interests shouldBe emptyList()
                }
            }

            context("온보딩 완료 유저 + 관심사 있음") {
                it("프로필과 관심사를 반환한다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        nativeLanguage = NativeLanguage.KO,
                    )
                    val tag = InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1)
                    val interest = UserInterest(account = account, interestTag = tag)

                    every { accountStore.findById(accountId) } returns account
                    every { userInterestStore.findByAccountId(accountId) } returns listOf(interest)

                    val result = useCase.execute(accountId)

                    result.account.nickname?.value shouldBe "테스트유저"
                    result.interests.size shouldBe 1
                    result.interests[0].interestTag.tagKey shouldBe "TRAVEL"
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    every { accountStore.findById(accountId) } returns null

                    shouldThrow<NotFoundException> {
                        useCase.execute(accountId)
                    }
                }
            }
        }
    }
}
