package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.interest.port.InterestTagStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ConflictException
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
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate

class CompleteOnboardingUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>(relaxed = true)
    private val interestTagStore = mockk<InterestTagStore>()
    private val userInterestStore = mockk<UserInterestStore>(relaxed = true)
    private val useCase = CompleteOnboardingUseCase(accountStore, interestTagStore, userInterestStore)

    private val validCommand = CompleteOnboardingCommand(
        nickname = "테스트닉네임",
        birthDate = LocalDate.of(2000, 1, 1),
        nation = Nation.KR,
        interestTagIds = listOf(1L, 2L),
    )

    private val testTags = listOf(
        InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
        InterestTag(id = 2, tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
    )

    init {
        afterEach { clearMocks(accountStore, interestTagStore, userInterestStore) }

        describe("execute") {
            context("유효한 온보딩 데이터") {
                it("계정 상태를 ACTIVE로 변경하고 관심사를 저장한다") {
                    val account = AccountFixture.create(id = 1)
                    every { accountStore.findById(1L) } returns account
                    every { accountStore.existsByNickname(Nickname("테스트닉네임")) } returns false
                    every { interestTagStore.findAllByIds(listOf(1L, 2L)) } returns testTags

                    useCase.execute(1L, validCommand)

                    account.accountStatus shouldBe AccountStatus.ACTIVE
                    account.nickname shouldBe Nickname("테스트닉네임")
                    account.nation shouldBe Nation.KR
                    account.nativeLanguage shouldBe com.chat.chingudachi.domain.account.NativeLanguage.KO
                    verify { accountStore.save(account) }
                    verify { userInterestStore.deleteByAccountId(1L) }
                    verify { userInterestStore.saveAll(match<List<UserInterest>> { it.size == 2 }) }
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    every { accountStore.findById(999L) } returns null

                    shouldThrow<NotFoundException> {
                        useCase.execute(999L, validCommand)
                    }
                }
            }

            context("이미 온보딩 완료된 계정") {
                it("ConflictException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.ACTIVE)
                    every { accountStore.findById(1L) } returns account

                    shouldThrow<ConflictException> {
                        useCase.execute(1L, validCommand)
                    }
                }
            }

            context("중복 닉네임") {
                it("ConflictException을 던진다") {
                    val account = AccountFixture.create(id = 1)
                    every { accountStore.findById(1L) } returns account
                    every { accountStore.existsByNickname(Nickname("테스트닉네임")) } returns true

                    shouldThrow<ConflictException> {
                        useCase.execute(1L, validCommand)
                    }
                }
            }

            context("관심사 미선택") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1)
                    every { accountStore.findById(1L) } returns account
                    every { accountStore.existsByNickname(Nickname("테스트닉네임")) } returns false

                    val emptyInterests = validCommand.copy(interestTagIds = emptyList())

                    shouldThrow<BadRequestException> {
                        useCase.execute(1L, emptyInterests)
                    }
                }
            }

            context("존재하지 않는 관심사 태그 ID") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1)
                    every { accountStore.findById(1L) } returns account
                    every { accountStore.existsByNickname(Nickname("테스트닉네임")) } returns false
                    every { interestTagStore.findAllByIds(listOf(1L, 999L)) } returns listOf(testTags[0])

                    val invalidTags = validCommand.copy(interestTagIds = listOf(1L, 999L))

                    shouldThrow<BadRequestException> {
                        useCase.execute(1L, invalidTags)
                    }
                }
            }
        }
    }
}
