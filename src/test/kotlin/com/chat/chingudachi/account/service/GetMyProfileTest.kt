package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
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
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class GetMyProfileTest : DescribeSpec() {
    private val accountRepository = mockk<AccountRepository>()
    private val termsAgreementRepository = mockk<AccountTermsAgreementRepository>()
    private val interestTagRepository = mockk<InterestTagRepository>()
    private val userInterestRepository = mockk<UserInterestRepository>()
    private val authTokenRepository = mockk<AuthTokenRepository>()
    private val accountService = AccountService(
        accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository, authTokenRepository,
    )

    init {
        afterEach { clearMocks(accountRepository, userInterestRepository) }

        describe("getMyProfile") {
            val accountId = 1L

            context("온보딩 미완료 유저") {
                it("기본 프로필과 빈 관심사를 반환한다") {
                    val account = AccountFixture.create(id = accountId)
                    every { accountRepository.findByIdOrNull(accountId) } returns account
                    every { userInterestRepository.findByAccountId(accountId) } returns emptyList()

                    val result = accountService.getMyProfile(accountId)

                    result.userId shouldBe accountId
                    result.onboardingRequired shouldBe true
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
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val tag = InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1)
                    val interest = UserInterest(account = account, interestTag = tag)

                    every { accountRepository.findByIdOrNull(accountId) } returns account
                    every { userInterestRepository.findByAccountId(accountId) } returns listOf(interest)

                    val result = accountService.getMyProfile(accountId)

                    result.nickname shouldBe "테스트유저"
                    result.interests.size shouldBe 1
                    result.interests[0] shouldBe "TRAVEL"
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    every { accountRepository.findByIdOrNull(accountId) } returns null

                    shouldThrow<NotFoundException> {
                        accountService.getMyProfile(accountId)
                    }
                }
            }
        }
    }
}
