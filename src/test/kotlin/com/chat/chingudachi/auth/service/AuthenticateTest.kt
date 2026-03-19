package com.chat.chingudachi.auth.service

import com.chat.chingudachi.auth.infra.OAuthClient
import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.auth.repository.AccountCredentialRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.AccountType
import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.CredentialType
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.core.domain.UnauthorizedException
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.OAuthUserInfoFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.Duration
import java.time.LocalDate

class AuthenticateTest : DescribeSpec() {
    private val oAuthClient = mockk<OAuthClient>()
    private val accountRepository = mockk<AccountRepository>(relaxUnitFun = true)
    private val accountCredentialRepository = mockk<AccountCredentialRepository>(relaxUnitFun = true)
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
        beforeEach {
            clearMocks(oAuthClient, accountRepository, accountCredentialRepository, authTokenRepository, tokenProvider)
            every { tokenProvider.refreshTokenExpiry } returns Duration.ofDays(30)
        }

        describe("authenticate") {
            val command = AuthenticateCommand(code = "valid-auth-code")
            val oAuthUserInfo = OAuthUserInfoFixture.create()

            context("신규 유저인 경우") {
                it("Account + Credential을 생성하고, NOT_CONSENT 상태, onboardingRequired=true를 반환한다") {
                    every { oAuthClient.authenticate("valid-auth-code", null) } returns oAuthUserInfo
                    every {
                        accountCredentialRepository.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns null
                    val savedAccountSlot = slot<com.chat.chingudachi.account.domain.Account>()
                    every { accountRepository.save(capture(savedAccountSlot)) } answers { savedAccountSlot.captured }
                    every { accountCredentialRepository.save(any()) } answers { firstArg() }
                    every { tokenProvider.createAccessToken(any()) } returns "access-token"
                    every { tokenProvider.createRefreshToken(any()) } returns "refresh-token"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    val result = authService.authenticate(command)

                    result.accessToken shouldBe "access-token"
                    result.refreshToken shouldBe "refresh-token"
                    result.onboardingRequired shouldBe true

                    savedAccountSlot.captured.accountStatus shouldBe AccountStatus.NOT_CONSENT
                    savedAccountSlot.captured.accountType shouldBe AccountType.USER
                    savedAccountSlot.captured.email shouldBe oAuthUserInfo.email

                    verify { authTokenRepository.deleteByAccountId(any()) }
                    verify { authTokenRepository.save(any()) }
                }
            }

            context("기존 유저인 경우") {
                it("기존 Account를 사용하고 새 Account를 생성하지 않는다") {
                    val existingAccount = AccountFixture.create(id = 10L)
                    val existingCredential = AccountCredential(
                        id = 1L,
                        accountId = 10L,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code", null) } returns oAuthUserInfo
                    every {
                        accountCredentialRepository.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { accountRepository.findByIdOrNull(10L) } returns existingAccount
                    every { tokenProvider.createAccessToken(10L) } returns "existing-access"
                    every { tokenProvider.createRefreshToken(10L) } returns "existing-refresh"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    val result = authService.authenticate(command)

                    result.accessToken shouldBe "existing-access"
                    result.refreshToken shouldBe "existing-refresh"
                    result.onboardingRequired shouldBe true

                    verify(exactly = 0) { accountRepository.save(any()) }
                    verify { authTokenRepository.deleteByAccountId(10L) }
                }
            }

            context("온보딩 완료 유저인 경우") {
                it("onboardingRequired=false를 반환한다") {
                    val completedAccount = AccountFixture.create(
                        id = 20L,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val existingCredential = AccountCredential(
                        id = 2L,
                        accountId = 20L,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code", null) } returns oAuthUserInfo
                    every {
                        accountCredentialRepository.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { accountRepository.findByIdOrNull(20L) } returns completedAccount
                    every { tokenProvider.createAccessToken(20L) } returns "completed-access"
                    every { tokenProvider.createRefreshToken(20L) } returns "completed-refresh"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    val result = authService.authenticate(command)

                    result.onboardingRequired shouldBe false
                }
            }

            context("기존 refresh token이 있는 경우") {
                it("기존 토큰을 삭제하고 새로 생성한다") {
                    val existingAccount = AccountFixture.create(id = 30L)
                    val existingCredential = AccountCredential(
                        id = 3L,
                        accountId = 30L,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code", null) } returns oAuthUserInfo
                    every {
                        accountCredentialRepository.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { accountRepository.findByIdOrNull(30L) } returns existingAccount
                    every { tokenProvider.createAccessToken(30L) } returns "new-access"
                    every { tokenProvider.createRefreshToken(30L) } returns "new-refresh"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    authService.authenticate(command)

                    verify(exactly = 1) { authTokenRepository.deleteByAccountId(30L) }
                    verify(exactly = 1) { authTokenRepository.save(any()) }
                }
            }

            context("OAuth 인증 실패 시") {
                it("예외를 전파한다") {
                    every { oAuthClient.authenticate("invalid-code", null) } throws
                        UnauthorizedException()

                    shouldThrow<UnauthorizedException> {
                        authService.authenticate(AuthenticateCommand(code = "invalid-code"))
                    }
                }
            }
        }
    }
}
