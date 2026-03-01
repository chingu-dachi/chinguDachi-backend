package com.chat.chingudachi.infrastructure.jwt

import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.time.Duration
import java.util.Base64

class JwtProviderTest : DescribeSpec() {
    private val testSecret = Base64.getEncoder().encodeToString(
        "chingudachi-test-secret-key-256b".toByteArray(),
    )
    private val differentSecret = Base64.getEncoder().encodeToString(
        "different-test-secret-key-256bit".toByteArray(),
    )

    private val jwtProvider = JwtProvider(
        JwtProperties(
            secret = testSecret,
            accessTokenExpiry = Duration.ofHours(1),
            refreshTokenExpiry = Duration.ofDays(30),
        ),
    )

    init {
        describe("createAccessToken") {
            it("accountId로 access token을 생성한다") {
                val token = jwtProvider.createAccessToken(1L)

                token.shouldNotBeEmpty()
            }

            it("생성된 토큰에서 accountId를 추출할 수 있다") {
                val accountId = 42L
                val token = jwtProvider.createAccessToken(accountId)

                jwtProvider.parseAccountId(token) shouldBe accountId
            }
        }

        describe("createRefreshToken") {
            it("accountId로 refresh token을 생성한다") {
                val token = jwtProvider.createRefreshToken(1L)

                token.shouldNotBeEmpty()
            }

            it("access token과 refresh token은 서로 다르다") {
                val accountId = 1L
                val accessToken = jwtProvider.createAccessToken(accountId)
                val refreshToken = jwtProvider.createRefreshToken(accountId)

                accessToken shouldNotBe refreshToken
            }
        }

        describe("validateToken") {
            it("유효한 토큰이면 true를 반환한다") {
                val token = jwtProvider.createAccessToken(1L)

                jwtProvider.validateToken(token) shouldBe true
            }

            it("만료된 토큰이면 false를 반환한다") {
                val expiredProvider = JwtProvider(
                    JwtProperties(
                        secret = testSecret,
                        accessTokenExpiry = Duration.ZERO,
                        refreshTokenExpiry = Duration.ZERO,
                    ),
                )
                val token = expiredProvider.createAccessToken(1L)

                expiredProvider.validateToken(token) shouldBe false
            }

            it("다른 키로 서명된 토큰이면 false를 반환한다") {
                val otherProvider = JwtProvider(
                    JwtProperties(
                        secret = differentSecret,
                        accessTokenExpiry = Duration.ofHours(1),
                        refreshTokenExpiry = Duration.ofDays(30),
                    ),
                )
                val token = otherProvider.createAccessToken(1L)

                jwtProvider.validateToken(token) shouldBe false
            }

            it("형식이 잘못된 토큰이면 false를 반환한다") {
                jwtProvider.validateToken("invalid.token.here") shouldBe false
            }
        }

        describe("parseAccountId") {
            it("정상 토큰에서 accountId를 추출한다") {
                val accountId = 99L
                val token = jwtProvider.createAccessToken(accountId)

                jwtProvider.parseAccountId(token) shouldBe accountId
            }

            it("만료된 토큰이면 AUTH_TOKEN_EXPIRED 예외를 던진다") {
                val expiredProvider = JwtProvider(
                    JwtProperties(
                        secret = testSecret,
                        accessTokenExpiry = Duration.ZERO,
                        refreshTokenExpiry = Duration.ZERO,
                    ),
                )
                val token = expiredProvider.createAccessToken(1L)

                val exception = shouldThrow<UnauthorizedException> {
                    jwtProvider.parseAccountId(token)
                }
                exception.errorCode shouldBe ErrorCode.AUTH_TOKEN_EXPIRED
            }

            it("잘못된 토큰이면 AUTH_TOKEN_INVALID 예외를 던진다") {
                val exception = shouldThrow<UnauthorizedException> {
                    jwtProvider.parseAccountId("invalid-token")
                }
                exception.errorCode shouldBe ErrorCode.AUTH_TOKEN_INVALID
            }

            it("다른 키로 서명된 토큰이면 AUTH_TOKEN_INVALID 예외를 던진다") {
                val otherProvider = JwtProvider(
                    JwtProperties(
                        secret = differentSecret,
                        accessTokenExpiry = Duration.ofHours(1),
                        refreshTokenExpiry = Duration.ofDays(30),
                    ),
                )
                val token = otherProvider.createAccessToken(1L)

                val exception = shouldThrow<UnauthorizedException> {
                    jwtProvider.parseAccountId(token)
                }
                exception.errorCode shouldBe ErrorCode.AUTH_TOKEN_INVALID
            }
        }
    }
}
