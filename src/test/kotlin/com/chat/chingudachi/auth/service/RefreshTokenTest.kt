package com.chat.chingudachi.auth.service

import com.chat.chingudachi.auth.infra.OAuthClient
import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.auth.repository.AccountCredentialRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.UnauthorizedException
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.AuthTokenFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class RefreshTokenTest : DescribeSpec() {
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
        beforeEach {
            clearMocks(accountRepository, authTokenRepository, tokenProvider)
            every { tokenProvider.refreshTokenExpiry } returns Duration.ofDays(30)
        }

        describe("refresh") {
            val oldRefreshToken = "old-refresh-token"
            val accountId = 1L

            context("유효한 refresh token인 경우") {
                it("기존 토큰을 삭제하고 새 토큰 쌍을 발급한다") {
                    val account = AccountFixture.create(id = accountId)
                    val authToken = AuthTokenFixture.create(
                        accountId = accountId,
                        refreshToken = oldRefreshToken,
                        expiresAt = Instant.now().plus(Duration.ofDays(29)),
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenRepository.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns authToken
                    every { accountRepository.findByIdOrNull(accountId) } returns account
                    every { tokenProvider.createAccessToken(accountId) } returns "new-access-token"
                    every { tokenProvider.createRefreshToken(accountId) } returns "new-refresh-token"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    val result = authService.refresh(oldRefreshToken)

                    result.accessToken shouldBe "new-access-token"
                    result.refreshToken shouldBe "new-refresh-token"
                    result.onboardingRequired shouldBe true

                    verify(exactly = 1) { authTokenRepository.deleteByAccountId(accountId) }
                    verify(exactly = 1) { authTokenRepository.save(any()) }
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
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val authToken = AuthTokenFixture.create(
                        accountId = accountId,
                        refreshToken = oldRefreshToken,
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenRepository.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns authToken
                    every { accountRepository.findByIdOrNull(accountId) } returns completedAccount
                    every { tokenProvider.createAccessToken(accountId) } returns "new-access"
                    every { tokenProvider.createRefreshToken(accountId) } returns "new-refresh"
                    every { authTokenRepository.save(any()) } answers { firstArg() }

                    val result = authService.refresh(oldRefreshToken)

                    result.onboardingRequired shouldBe false
                }
            }

            context("DB에 없는 refresh token인 경우") {
                it("AUTH_REFRESH_TOKEN_INVALID 예외를 던진다") {
                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenRepository.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns null

                    val exception = shouldThrow<UnauthorizedException> {
                        authService.refresh(oldRefreshToken)
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_REFRESH_TOKEN_INVALID
                }
            }

            context("만료된 refresh token인 경우") {
                it("AUTH_REFRESH_TOKEN_EXPIRED 예외를 던지고 기존 토큰을 삭제한다") {
                    val expiredToken = AuthTokenFixture.create(
                        accountId = accountId,
                        refreshToken = oldRefreshToken,
                        expiresAt = Instant.now().minus(Duration.ofHours(1)),
                    )

                    every { tokenProvider.parseAccountId(oldRefreshToken) } returns accountId
                    every {
                        authTokenRepository.findByAccountIdAndRefreshToken(accountId, oldRefreshToken)
                    } returns expiredToken

                    val exception = shouldThrow<UnauthorizedException> {
                        authService.refresh(oldRefreshToken)
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED
                    verify(exactly = 1) { authTokenRepository.deleteByAccountId(accountId) }
                }
            }

            context("JWT 파싱 실패 시") {
                it("TokenProvider의 예외를 전파한다") {
                    every { tokenProvider.parseAccountId(oldRefreshToken) } throws
                        UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)

                    shouldThrow<UnauthorizedException> {
                        authService.refresh(oldRefreshToken)
                    }
                }
            }
        }
    }
}
