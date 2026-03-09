package com.chat.chingudachi.auth.application

import com.chat.chingudachi.auth.application.port.AuthTokenStore
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class LogoutUseCaseTest : DescribeSpec() {
    private val authTokenStore = mockk<AuthTokenStore>()
    private val useCase = LogoutUseCase(authTokenStore)

    init {
        afterEach { clearMocks(authTokenStore) }

        describe("execute") {
            it("모든 refresh token을 삭제한다") {
                val accountId = 1L
                every { authTokenStore.deleteByAccountId(accountId) } returns Unit

                useCase.execute(accountId)

                verify(exactly = 1) { authTokenStore.deleteByAccountId(accountId) }
            }
        }
    }
}
