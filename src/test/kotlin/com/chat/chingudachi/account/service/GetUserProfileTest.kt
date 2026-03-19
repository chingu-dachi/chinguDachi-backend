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

class GetUserProfileTest : DescribeSpec() {
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

        describe("getUserProfile") {
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

                    every { accountRepository.findByIdOrNull(userId) } returns account
                    every { userInterestRepository.findByAccountId(userId) } returns listOf(interest)

                    val result = accountService.getUserProfile(userId)

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
                    every { accountRepository.findByIdOrNull(userId) } returns account

                    shouldThrow<NotFoundException> {
                        accountService.getUserProfile(userId)
                    }
                }
            }

            context("INACTIVE 유저") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.INACTIVE,
                    )
                    every { accountRepository.findByIdOrNull(userId) } returns account

                    shouldThrow<NotFoundException> {
                        accountService.getUserProfile(userId)
                    }
                }
            }

            context("SUSPENDED 유저") {
                it("NotFoundException을 던진다") {
                    val account = AccountFixture.create(
                        id = userId,
                        accountStatus = AccountStatus.SUSPENDED,
                    )
                    every { accountRepository.findByIdOrNull(userId) } returns account

                    shouldThrow<NotFoundException> {
                        accountService.getUserProfile(userId)
                    }
                }
            }

            context("존재하지 않는 유저") {
                it("NotFoundException을 던진다") {
                    every { accountRepository.findByIdOrNull(userId) } returns null

                    shouldThrow<NotFoundException> {
                        accountService.getUserProfile(userId)
                    }
                }
            }
        }
    }
}
