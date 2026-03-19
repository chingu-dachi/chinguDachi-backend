package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.interest.repository.InterestTagRepository
import com.chat.chingudachi.interest.repository.UserInterestRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant

class DeleteAccountTest : DescribeSpec() {
    private val accountRepository = mockk<AccountRepository>()
    private val termsAgreementRepository = mockk<AccountTermsAgreementRepository>()
    private val interestTagRepository = mockk<InterestTagRepository>()
    private val userInterestRepository = mockk<UserInterestRepository>()
    private val authTokenRepository = mockk<AuthTokenRepository>()
    private val accountService = AccountService(
        accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository, authTokenRepository,
    )

    init {
        afterEach { clearMocks(accountRepository, authTokenRepository, userInterestRepository) }

        describe("deleteAccount") {
            context("존재하는 계정") {
                it("계정을 soft delete하고 토큰과 관심사를 삭제한다") {
                    val accountId = 1L
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                    )
                    every { accountRepository.findByIdOrNull(accountId) } returns account
                    every { accountRepository.save(account) } returns account
                    every { authTokenRepository.deleteByAccountId(accountId) } returns Unit
                    every { userInterestRepository.deleteByAccountId(accountId) } returns Unit

                    accountService.deleteAccount(accountId)

                    account.deletedAt shouldNotBe null
                    account.accountStatus shouldBe AccountStatus.INACTIVE
                    account.nickname shouldBe null
                    verify(exactly = 1) { accountRepository.save(account) }
                    verify(exactly = 1) { authTokenRepository.deleteByAccountId(accountId) }
                    verify(exactly = 1) { userInterestRepository.deleteByAccountId(accountId) }
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    val accountId = 999L
                    every { accountRepository.findByIdOrNull(accountId) } returns null

                    shouldThrow<NotFoundException> {
                        accountService.deleteAccount(accountId)
                    }
                }
            }

            context("이미 삭제된 계정") {
                it("ConflictException을 던진다") {
                    val accountId = 1L
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.INACTIVE,
                    )
                    account.deletedAt = Instant.now()
                    every { accountRepository.findByIdOrNull(accountId) } returns account

                    shouldThrow<ConflictException> {
                        accountService.deleteAccount(accountId)
                    }
                }
            }
        }
    }
}
