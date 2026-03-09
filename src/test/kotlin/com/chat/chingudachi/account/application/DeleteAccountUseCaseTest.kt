package com.chat.chingudachi.account.application

import com.chat.chingudachi.auth.application.port.AuthTokenStore
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.account.application.port.UserInterestStore
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class DeleteAccountUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>()
    private val authTokenStore = mockk<AuthTokenStore>()
    private val userInterestStore = mockk<UserInterestStore>()
    private val useCase = DeleteAccountUseCase(accountStore, authTokenStore, userInterestStore)

    init {
        afterEach { clearMocks(accountStore, authTokenStore, userInterestStore) }

        describe("execute") {
            context("존재하는 계정") {
                it("계정을 soft delete하고 토큰과 관심사를 삭제한다") {
                    val accountId = 1L
                    val account = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                    )
                    every { accountStore.getById(accountId) } returns account
                    every { accountStore.save(account) } returns account
                    every { authTokenStore.deleteByAccountId(accountId) } returns Unit
                    every { userInterestStore.deleteByAccountId(accountId) } returns Unit

                    useCase.execute(accountId)

                    account.deletedAt shouldNotBe null
                    account.accountStatus shouldBe AccountStatus.INACTIVE
                    account.nickname shouldBe null
                    verify(exactly = 1) { accountStore.save(account) }
                    verify(exactly = 1) { authTokenStore.deleteByAccountId(accountId) }
                    verify(exactly = 1) { userInterestStore.deleteByAccountId(accountId) }
                }
            }

            context("존재하지 않는 계정") {
                it("NotFoundException을 던진다") {
                    val accountId = 999L
                    every { accountStore.getById(accountId) } throws NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

                    shouldThrow<NotFoundException> {
                        useCase.execute(accountId)
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
                    every { accountStore.getById(accountId) } returns account

                    shouldThrow<ConflictException> {
                        useCase.execute(accountId)
                    }
                }
            }
        }
    }
}
