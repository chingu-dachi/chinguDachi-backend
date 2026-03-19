package com.chat.chingudachi.auth.service

import com.chat.chingudachi.auth.infra.OAuthClient
import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.auth.repository.AccountCredentialRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.account.repository.AccountRepository
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify

class LogoutTest : DescribeSpec() {
    private val oAuthClient = mockk<OAuthClient>()
    private val accountRepository = mockk<AccountRepository>()
    private val accountCredentialRepository = mockk<AccountCredentialRepository>()
    private val authTokenRepository = mockk<AuthTokenRepository>(relaxUnitFun = true)
    private val tokenProvider = mockk<TokenProvider>()

    private val authService = AuthService(
        oAuthClient = oAuthClient,
        accountRepository = accountRepository,
        accountCredentialRepository = accountCredentialRepository,
        authTokenRepository = authTokenRepository,
        tokenProvider = tokenProvider,
    )

    init {
        afterEach { clearMocks(authTokenRepository) }

        describe("logout") {
            it("모든 refresh token을 삭제한다") {
                val accountId = 1L

                authService.logout(accountId)

                verify(exactly = 1) { authTokenRepository.deleteByAccountId(accountId) }
            }
        }
    }
}
