package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.NativeLanguage
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.AuthTokenFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class RefreshTokenUseCaseTest : DescribeSpec() {
    private val authTokenStore = mockk<AuthTokenStore>(relaxUnitFun = true)
    private val tokenProvider = mockk<TokenProvider>()

    private val useCase = RefreshTokenUseCase(
        authTokenStore = authTokenStore,
        tokenProvider = tokenProvider,
    )

    init {
        beforeEach {
            clearMocks(authTokenStore, tokenProvider)
            every { tokenProvider.refreshTokenExpiry } returns Duration.ofDays(30)
        }

        describe("refresh") {
            val oldRefreshToken = "old-refresh-token"
            val accountId = 1L

            context("유효한 refresh token인 경우") {
                it("기존 토큰을 삭제하고 새 토큰 쌍을 발급한다") {
                    val account = AccountFixture.create(id = accountId)
                    val authToken = AuthTokenFixture.create(
                        account = account,
                        refreshToken = oldRefreshToken,
                        expiresAt = Instant.now().plus(Duration.ofDays(29)),
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenStore.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns authToken
                    every { tokenProvider.createAccessToken(accountId) } returns "new-access-token"
                    every { tokenProvider.createRefreshToken(accountId) } returns "new-refresh-token"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    val result = useCase.refresh(oldRefreshToken)

                    result.accessToken shouldBe "new-access-token"
                    result.refreshToken shouldBe "new-refresh-token"
                    result.onboardingRequired shouldBe true

                    verify(exactly = 1) { authTokenStore.deleteByAccountId(accountId) }
                    verify(exactly = 1) { authTokenStore.save(any()) }
                }
            }

            context("온보딩 완료 유저의 refresh인 경우") {
                it("onboardingRequired=false를 반환한다") {
                    val completedAccount = AccountFixture.create(
                        id = accountId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("테스트유저"),
                        birthDate = LocalDate.of(1995, 5, 15),
                        nation = Nation.KR,
                        nativeLanguage = NativeLanguage.KO,
                    )
                    val authToken = AuthTokenFixture.create(
                        account = completedAccount,
                        refreshToken = oldRefreshToken,
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenStore.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns authToken
                    every { tokenProvider.createAccessToken(accountId) } returns "new-access"
                    every { tokenProvider.createRefreshToken(accountId) } returns "new-refresh"
                    every { authTokenStore.save(any()) } answers { firstArg() }

                    val result = useCase.refresh(oldRefreshToken)

                    result.onboardingRequired shouldBe false
                }
            }

            context("DB에 없는 refresh token인 경우") {
                it("AUTH_REFRESH_TOKEN_INVALID 예외를 던진다") {
                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenStore.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns null

                    val exception = shouldThrow<UnauthorizedException> {
                        useCase.refresh(oldRefreshToken)
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_REFRESH_TOKEN_INVALID
                }
            }

            context("만료된 refresh token인 경우") {
                it("AUTH_REFRESH_TOKEN_EXPIRED 예외를 던지고 기존 토큰을 삭제한다") {
                    val account = AccountFixture.create(id = accountId)
                    val expiredToken = AuthTokenFixture.create(
                        account = account,
                        refreshToken = oldRefreshToken,
                        expiresAt = Instant.now().minus(Duration.ofHours(1)),
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenStore.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns expiredToken

                    val exception = shouldThrow<UnauthorizedException> {
                        useCase.refresh(oldRefreshToken)
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED
                    verify(exactly = 1) { authTokenStore.deleteByAccountId(accountId) }
                }
            }

            context("JWT 파싱 실패 시") {
                it("TokenProvider의 예외를 전파한다") {
                    every { tokenProvider.parseAccountId(oldRefreshToken) } throws
                        UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)

                    shouldThrow<UnauthorizedException> {
                        useCase.refresh(oldRefreshToken)
                    }
                }
            }
        }
    }
}
