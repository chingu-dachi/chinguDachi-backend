package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.interest.repository.InterestTagRepository
import com.chat.chingudachi.interest.repository.UserInterestRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class CheckNicknameTest : DescribeSpec() {
    private val accountRepository = mockk<AccountRepository>()
    private val termsAgreementRepository = mockk<AccountTermsAgreementRepository>()
    private val interestTagRepository = mockk<InterestTagRepository>()
    private val userInterestRepository = mockk<UserInterestRepository>()
    private val authTokenRepository = mockk<AuthTokenRepository>()
    private val accountService = AccountService(
        accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository, authTokenRepository,
    )

    init {
        afterEach { clearMocks(accountRepository) }

        describe("checkNickname") {
            context("사용 가능한 닉네임") {
                it("true를 반환한다") {
                    every { accountRepository.existsByNickname(Nickname("새닉네임")) } returns false

                    accountService.checkNickname("새닉네임") shouldBe true
                }
            }

            context("이미 사용 중인 닉네임") {
                it("false를 반환한다") {
                    every { accountRepository.existsByNickname(Nickname("중복닉네임")) } returns true

                    accountService.checkNickname("중복닉네임") shouldBe false
                }
            }

            context("유효하지 않은 닉네임") {
                it("빈 문자열이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        accountService.checkNickname("")
                    }
                }

                it("1글자이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        accountService.checkNickname("아")
                    }
                }

                it("13자 이상이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        accountService.checkNickname("일이삼사오육칠팔구십일이삼")
                    }
                }

                it("공백이 포함되면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        accountService.checkNickname("닉 네임")
                    }
                }
            }
        }
    }
}
