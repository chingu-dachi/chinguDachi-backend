package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.domain.account.Nickname
import io.kotest.assertions.throwables.shouldThrow
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
                    every { accountStore.existsByNickname(Nickname("새닉네임")) } returns false

                    useCase.execute("새닉네임") shouldBe true
                }
            }

            context("이미 사용 중인 닉네임") {
                it("false를 반환한다") {
                    every { accountStore.existsByNickname(Nickname("중복닉네임")) } returns true

                    useCase.execute("중복닉네임") shouldBe false
                }
            }

            context("유효하지 않은 닉네임") {
                it("빈 문자열이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        useCase.execute("")
                    }
                }

                it("1글자이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        useCase.execute("아")
                    }
                }

                it("13자 이상이면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        useCase.execute("일이삼사오육칠팔구십일이삼")
                    }
                }

                it("공백이 포함되면 IllegalArgumentException을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        useCase.execute("닉 네임")
                    }
                }
            }
        }
    }
}
