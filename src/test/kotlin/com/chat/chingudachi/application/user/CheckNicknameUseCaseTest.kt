package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class CheckNicknameUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>()
    private val useCase = CheckNicknameUseCase(accountStore)

    init {
        afterEach { clearMocks(accountStore) }

        describe("execute") {
            context("사용 가능한 닉네임") {
                it("true를 반환한다") {
                    every { accountStore.existsByNickname("새닉네임") } returns false

                    useCase.execute("새닉네임") shouldBe true
                }
            }

            context("이미 사용 중인 닉네임") {
                it("false를 반환한다") {
                    every { accountStore.existsByNickname("중복닉네임") } returns true

                    useCase.execute("중복닉네임") shouldBe false
                }
            }
        }
    }
}
