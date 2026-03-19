package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.interest.domain.InterestTag
import com.chat.chingudachi.interest.domain.UserInterest
import com.chat.chingudachi.interest.repository.InterestTagRepository
import com.chat.chingudachi.interest.repository.UserInterestRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class CompleteOnboardingTest : DescribeSpec() {
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val termsAgreementRepository = mockk<AccountTermsAgreementRepository>()
    private val interestTagRepository = mockk<InterestTagRepository>()
    private val userInterestRepository = mockk<UserInterestRepository>(relaxed = true)
    private val authTokenRepository = mockk<AuthTokenRepository>()
    private val accountService = AccountService(
        accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository, authTokenRepository,
    )

    private val validCommand = CompleteOnboardingCommand(
        nickname = "테스트닉네임",
        birthYear = 2000,
        nationality = "KR",
        interests = listOf("TRAVEL", "FOOD"),
    )

    private val testTags = listOf(
        InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
        InterestTag(id = 2, tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
    )

    init {
        beforeEach {
            clearMocks(accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository)
            every { accountRepository.save(any<Account>()) } answers { firstArg() }
        }

        describe("completeOnboarding") {
            context("유효한 온보딩 데이터") {
                it("계정 상태를 ACTIVE로 변경하고 관심사를 저장한다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true
                    every { accountRepository.existsByNickname(Nickname("테스트닉네임")) } returns false
                    every { interestTagRepository.findAllByTagKeyIn(listOf("TRAVEL", "FOOD")) } returns testTags

                    accountService.completeOnboarding(1L, validCommand)

                    account.accountStatus shouldBe AccountStatus.ACTIVE
                    account.nickname shouldBe Nickname("테스트닉네임")
                    account.nation shouldBe Nation.KR
                    account.translateLanguage shouldBe TranslateLanguage.JA
                    verify { accountRepository.save(account) }
                    verify { userInterestRepository.deleteByAccountId(1L) }
                    verify { userInterestRepository.saveAll(match<List<UserInterest>> { it.size == 2 }) }
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    every { accountRepository.findByIdOrNull(999L) } returns null

                    shouldThrow<NotFoundException> {
                        accountService.completeOnboarding(999L, validCommand)
                    }
                }
            }

            context("동의 미완료 계정") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(id = 1)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns false

                    val exception = shouldThrow<NotFoundException> {
                        accountService.completeOnboarding(1L, validCommand)
                    }
                    exception.errorCode shouldBe ErrorCode.CONSENT_NOT_FOUND
                }
            }

            context("이미 온보딩 완료된 계정") {
                it("ConflictException을 던진다") {
                    val account = AccountFixture.create(
                        id = 1,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("기존닉네임"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true

                    shouldThrow<ConflictException> {
                        accountService.completeOnboarding(1L, validCommand)
                    }
                }
            }

            context("중복 닉네임") {
                it("ConflictException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true
                    every { accountRepository.existsByNickname(Nickname("테스트닉네임")) } returns true

                    shouldThrow<ConflictException> {
                        accountService.completeOnboarding(1L, validCommand)
                    }
                }
            }

            context("관심사 미선택") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true
                    every { accountRepository.existsByNickname(Nickname("테스트닉네임")) } returns false

                    val emptyInterests = validCommand.copy(interests = emptyList())

                    shouldThrow<BadRequestException> {
                        accountService.completeOnboarding(1L, emptyInterests)
                    }
                }
            }

            context("유효하지 않은 nationality") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true

                    val invalidNationality = validCommand.copy(nationality = "US")

                    shouldThrow<BadRequestException> {
                        accountService.completeOnboarding(1L, invalidNationality)
                    }
                }
            }

            context("유효하지 않은 birthYear") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true

                    val invalidBirthYear = validCommand.copy(birthYear = 2030)

                    shouldThrow<BadRequestException> {
                        accountService.completeOnboarding(1L, invalidBirthYear)
                    }
                }
            }

            context("존재하지 않는 관심사 태그 키") {
                it("BadRequestException을 던진다") {
                    val account = AccountFixture.create(id = 1, accountStatus = AccountStatus.NOT_ENOUGH_DETAIL)
                    every { accountRepository.findByIdOrNull(1L) } returns account
                    every { termsAgreementRepository.existsByAccountId(1L) } returns true
                    every { accountRepository.existsByNickname(Nickname("테스트닉네임")) } returns false
                    every { interestTagRepository.findAllByTagKeyIn(listOf("TRAVEL", "INVALID")) } returns listOf(testTags[0])

                    val invalidTags = validCommand.copy(interests = listOf("TRAVEL", "INVALID"))

                    shouldThrow<BadRequestException> {
                        accountService.completeOnboarding(1L, invalidTags)
                    }
                }
            }
        }
    }
}
