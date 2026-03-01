package com.chat.chingudachi.presentation.auth

import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import com.chat.chingudachi.domain.common.UnauthorizedException
import com.chat.chingudachi.infrastructure.persistence.account.AccountCredentialRepository
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import com.chat.chingudachi.infrastructure.persistence.auth.AuthTokenRepository
import com.chat.chingudachi.support.MockOAuthConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.clearMocks
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.servlet.http.Cookie
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Import(MockOAuthConfig::class)
class AuthControllerTest(
    private val mockMvc: MockMvc,
    private val oAuthClient: OAuthClient,
    private val authTokenRepository: AuthTokenRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val accountRepository: AccountRepository,
) : DescribeSpec() {
    init {
        afterEach {
            clearMocks(oAuthClient)
            authTokenRepository.deleteAll()
            accountCredentialRepository.deleteAll()
            accountRepository.deleteAll()
        }

        describe("POST /api/auth/google") {
            context("유효한 authorization code인 경우") {
                it("200 + 액세스 토큰 + 리프레시 토큰 쿠키를 반환한다") {
                    every { oAuthClient.authenticate("valid-code") } returns
                        OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "test@example.com")

                    val result = mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"valid-code"}"""
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.accessToken") { isNotEmpty() }
                        jsonPath("$.data.onboardingRequired") { value(true) }
                    }.andReturn()

                    val setCookieHeader = result.response.getHeader("Set-Cookie")!!
                    setCookieHeader shouldStartWith "refreshToken="
                    setCookieHeader.contains("HttpOnly") shouldBe true
                    setCookieHeader.contains("Secure") shouldBe true
                }
            }

            context("유효하지 않은 authorization code인 경우") {
                it("401을 반환한다") {
                    every { oAuthClient.authenticate("invalid-code") } throws
                        UnauthorizedException()

                    mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"invalid-code"}"""
                    }.andExpect {
                        status { isUnauthorized() }
                        jsonPath("$.isSuccess") { value(false) }
                    }
                }
            }
        }

        describe("POST /api/auth/refresh") {
            context("유효한 refresh token 쿠키가 있는 경우") {
                it("새 토큰 쌍을 반환하고 새 쿠키를 설정한다") {
                    every { oAuthClient.authenticate("valid-code") } returns
                        OAuthUserInfo(OAuthProvider.GOOGLE, "google-456", "test@example.com")

                    val loginResult = mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"valid-code"}"""
                    }.andReturn()

                    val setCookieHeader = loginResult.response.getHeader("Set-Cookie")!!
                    val refreshToken = setCookieHeader.substringAfter("refreshToken=").substringBefore(";")

                    val refreshResult = mockMvc.post("/api/auth/refresh") {
                        cookie(Cookie("refreshToken", refreshToken))
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.accessToken") { isNotEmpty() }
                        jsonPath("$.data.onboardingRequired") { value(true) }
                    }.andReturn()

                    val newSetCookie = refreshResult.response.getHeader("Set-Cookie")!!
                    newSetCookie shouldStartWith "refreshToken="
                }
            }

            context("refresh token 쿠키가 없는 경우") {
                it("401을 반환한다") {
                    mockMvc.post("/api/auth/refresh")
                        .andExpect {
                            status { isUnauthorized() }
                            jsonPath("$.isSuccess") { value(false) }
                        }
                }
            }
        }

        describe("JWT 인증 필터") {
            context("인증이 필요한 엔드포인트에 JWT 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/users/me")
                        .andExpect {
                            status { isUnauthorized() }
                            jsonPath("$.code") { value("UNAUTHORIZED") }
                            jsonPath("$.isSuccess") { value(false) }
                        }
                }
            }

            context("인증이 필요한 엔드포인트에 유효한 JWT로 요청하면") {
                it("인증을 통과한다") {
                    every { oAuthClient.authenticate("valid-code") } returns
                        OAuthUserInfo(OAuthProvider.GOOGLE, "google-789", "test@example.com")

                    val loginResult = mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"valid-code"}"""
                    }.andReturn()

                    val responseBody = loginResult.response.contentAsString
                    val accessToken = com.jayway.jsonpath.JsonPath.read<String>(
                        responseBody,
                        "$.data.accessToken",
                    )

                    mockMvc.get("/api/users/me") {
                        header("Authorization", "Bearer $accessToken")
                    }.andExpect {
                        status { isOk() }
                    }
                }
            }

            context("auth 엔드포인트는 인증 없이 접근 가능") {
                it("/api/auth/google은 permitAll") {
                    every { oAuthClient.authenticate("code") } returns
                        OAuthUserInfo(OAuthProvider.GOOGLE, "g-000", "test@example.com")

                    mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"code"}"""
                    }.andExpect {
                        status { isOk() }
                    }
                }
            }
        }
    }
}
