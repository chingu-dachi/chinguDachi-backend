package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.interest.port.InterestTagStore
import com.chat.chingudachi.application.user.command.UpdateProfileCommand
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ConflictException
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
import io.mockk.verify
import java.time.LocalDate

class UpdateProfileUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>(relaxed = true)
    private val interestTagStore = mockk<InterestTagStore>()
    private val userInterestStore = mockk<UserInterestStore>(relaxed = true)
    private val useCase = UpdateProfileUseCase(accountStore, interestTagStore, userInterestStore)

    private val testTags = listOf(
        InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
        InterestTag(id = 2, tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
    )

    init {
        afterEach { clearMocks(accountStore, interestTagStore, userInterestStore) }

        describe("execute") {
            val accountId = 1L

            context("닉네임 변경") {
                it("닉네임을 업데이트한다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("기존닉네임"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(nickname = "새닉네임")

                    every { accountStore.getById(accountId) } returns account
                    every { accountStore.existsByNickname(Nickname("새닉네임")) } returns false
                    every { userInterestStore.findByAccountId(accountId) } returns emptyList()

                    val result = useCase.execute(accountId, command)

                    result.account.nickname shouldBe Nickname("새닉네임")
                    verify { accountStore.save(account) }
                }
            }

            context("중복 닉네임") {
                it("ConflictException을 던진다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("기존닉네임"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(nickname = "중복닉네임")

                    every { accountStore.getById(accountId) } returns account
                    every { accountStore.existsByNickname(Nickname("중복닉네임")) } returns true

                    shouldThrow<ConflictException> {
                        useCase.execute(accountId, command)
                    }
                }
            }

            context("같은 닉네임으로 변경") {
                it("중복 체크를 건너뛰고 정상 처리한다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("기존닉네임"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(nickname = "기존닉네임")

                    every { accountStore.getById(accountId) } returns account
                    every { userInterestStore.findByAccountId(accountId) } returns emptyList()

                    val result = useCase.execute(accountId, command)

                    result.account.nickname shouldBe Nickname("기존닉네임")
                    verify(exactly = 0) { accountStore.existsByNickname(Nickname("기존닉네임")) }
                }
            }

            context("관심사 변경") {
                it("기존 관심사를 삭제하고 새로 저장한다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(interests = listOf("TRAVEL", "FOOD"))

                    every { accountStore.getById(accountId) } returns account
                    every { interestTagStore.findAllByTagKeys(listOf("TRAVEL", "FOOD")) } returns testTags
                    every { userInterestStore.findByAccountId(accountId) } returns testTags.map {
                        UserInterest(account = account, interestTag = it)
                    }

                    val result = useCase.execute(accountId, command)

                    verify { userInterestStore.deleteByAccountId(accountId) }
                    verify { userInterestStore.saveAll(match<List<UserInterest>> { it.size == 2 }) }
                    result.interests.size shouldBe 2
                }
            }

            context("빈 관심사 목록") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(interests = emptyList())

                    every { accountStore.getById(accountId) } returns account

                    shouldThrow<BadRequestException> {
                        useCase.execute(accountId, command)
                    }
                }
            }

            context("존재하지 않는 관심사 태그") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(interests = listOf("TRAVEL", "INVALID"))

                    every { accountStore.getById(accountId) } returns account
                    every { interestTagStore.findAllByTagKeys(listOf("TRAVEL", "INVALID")) } returns listOf(testTags[0])

                    shouldThrow<BadRequestException> {
                        useCase.execute(accountId, command)
                    }
                }
            }

            context("bio, city 변경") {
                it("해당 필드만 업데이트한다") {
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val command = UpdateProfileCommand(bio = "새로운 자기소개", city = "서울")

                    every { accountStore.getById(accountId) } returns account
                    every { userInterestStore.findByAccountId(accountId) } returns emptyList()

                    val result = useCase.execute(accountId, command)

                    result.account.bio shouldBe "새로운 자기소개"
                    result.account.city shouldBe "서울"
                    result.account.nickname shouldBe Nickname("테스트유저")
                    verify { accountStore.save(account) }
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    val command = UpdateProfileCommand(bio = "test")

                    every { accountStore.getById(accountId) } throws NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

                    shouldThrow<NotFoundException> {
                        useCase.execute(accountId, command)
                    }
                }
            }
        }
    }
}
