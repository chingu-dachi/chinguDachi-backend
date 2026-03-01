package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.command.AuthenticateCommand
import com.chat.chingudachi.application.auth.port.AccountCredentialStore
import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.AccountType
import com.chat.chingudachi.domain.account.CredentialType
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.NativeLanguage
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.UnauthorizedException
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
import java.time.Duration
import java.time.LocalDate

class AuthenticateUseCaseTest : DescribeSpec() {
    private val oAuthClient = mockk<OAuthClient>()
    private val accountStore = mockk<AccountStore>(relaxUnitFun = true)
    private val accountCredentialStore = mockk<AccountCredentialStore>(relaxUnitFun = true)
    private val authTokenStore = mockk<AuthTokenStore>(relaxUnitFun = true)
    private val tokenProvider = mockk<TokenProvider>()

    private val useCase = AuthenticateUseCase(
        oAuthClient = oAuthClient,
        accountStore = accountStore,
        accountCredentialStore = accountCredentialStore,
        authTokenStore = authTokenStore,
        tokenProvider = tokenProvider,
    )

    init {
        beforeEach {
            clearMocks(oAuthClient, accountStore, accountCredentialStore, authTokenStore, tokenProvider)
            every { tokenProvider.refreshTokenExpiry } returns Duration.ofDays(30)
        }

        describe("authenticate") {
            val command = AuthenticateCommand(code = "valid-auth-code")
            val oAuthUserInfo = OAuthUserInfoFixture.create()

            context("신규 유저인 경우") {
                it("Account + Credential을 생성하고, NOT_CONSENT 상태, onboardingRequired=true를 반환한다") {
                    every { oAuthClient.authenticate("valid-auth-code") } returns oAuthUserInfo
                    every {
                        accountCredentialStore.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns null
                    val savedAccountSlot = slot<com.chat.chingudachi.domain.account.Account>()
                    every { accountStore.save(capture(savedAccountSlot)) } answers { savedAccountSlot.captured }
                    every { accountCredentialStore.save(any()) } answers { firstArg() }
                    every { tokenProvider.createAccessToken(any()) } returns "access-token"
                    every { tokenProvider.createRefreshToken(any()) } returns "refresh-token"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    val result = useCase.authenticate(command)

                    result.accessToken shouldBe "access-token"
                    result.refreshToken shouldBe "refresh-token"
                    result.onboardingRequired shouldBe true

                    savedAccountSlot.captured.accountStatus shouldBe AccountStatus.NOT_CONSENT
                    savedAccountSlot.captured.accountType shouldBe AccountType.USER
                    savedAccountSlot.captured.email shouldBe oAuthUserInfo.email

                    verify { authTokenStore.deleteByAccountId(any()) }
                    verify { authTokenStore.save(any()) }
                }
            }

            context("기존 유저인 경우") {
                it("기존 Account를 사용하고 새 Account를 생성하지 않는다") {
                    val existingAccount = AccountFixture.create(id = 10L)
                    val existingCredential = com.chat.chingudachi.domain.account.AccountCredential(
                        id = 1L,
                        account = existingAccount,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code") } returns oAuthUserInfo
                    every {
                        accountCredentialStore.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { tokenProvider.createAccessToken(10L) } returns "existing-access"
                    every { tokenProvider.createRefreshToken(10L) } returns "existing-refresh"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    val result = useCase.authenticate(command)

                    result.accessToken shouldBe "existing-access"
                    result.refreshToken shouldBe "existing-refresh"
                    result.onboardingRequired shouldBe true

                    verify(exactly = 0) { accountStore.save(any()) }
                    verify { authTokenStore.deleteByAccountId(10L) }
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
                        nativeLanguage = NativeLanguage.KO,
                    )
                    val existingCredential = com.chat.chingudachi.domain.account.AccountCredential(
                        id = 2L,
                        account = completedAccount,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code") } returns oAuthUserInfo
                    every {
                        accountCredentialStore.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { tokenProvider.createAccessToken(20L) } returns "completed-access"
                    every { tokenProvider.createRefreshToken(20L) } returns "completed-refresh"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    val result = useCase.authenticate(command)

                    result.onboardingRequired shouldBe false
                }
            }

            context("기존 refresh token이 있는 경우") {
                it("기존 토큰을 삭제하고 새로 생성한다") {
                    val existingAccount = AccountFixture.create(id = 30L)
                    val existingCredential = com.chat.chingudachi.domain.account.AccountCredential(
                        id = 3L,
                        account = existingAccount,
                        credentialType = CredentialType.GOOGLE_OAUTH,
                        oauthKey = oAuthUserInfo.providerUserId,
                    )

                    every { oAuthClient.authenticate("valid-auth-code") } returns oAuthUserInfo
                    every {
                        accountCredentialStore.findByCredentialTypeAndOauthKey(
                            CredentialType.GOOGLE_OAUTH,
                            oAuthUserInfo.providerUserId,
                        )
                    } returns existingCredential
                    every { tokenProvider.createAccessToken(30L) } returns "new-access"
                    every { tokenProvider.createRefreshToken(30L) } returns "new-refresh"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    useCase.authenticate(command)

                    verify(exactly = 1) { authTokenStore.deleteByAccountId(30L) }
                    verify(exactly = 1) { authTokenStore.save(any()) }
                }
            }

            context("OAuth 인증 실패 시") {
                it("예외를 전파한다") {
                    every { oAuthClient.authenticate("invalid-code") } throws
                        UnauthorizedException()

                    shouldThrow<UnauthorizedException> {
                        useCase.authenticate(AuthenticateCommand(code = "invalid-code"))
                    }
                }
            }
        }
    }
}
