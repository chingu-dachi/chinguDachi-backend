package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.infrastructure.persistence.account.AccountCredentialRepository
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import com.chat.chingudachi.infrastructure.persistence.auth.AuthTokenRepository
import com.chat.chingudachi.support.MockOAuthConfig
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
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
class UserControllerTest(
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

        describe("GET /api/users/me") {
            context("인증된 유저가 요청하면") {
                it("프로필 정보를 반환한다") {
                    every { oAuthClient.authenticate("valid-code") } returns
                        OAuthUserInfo(OAuthProvider.GOOGLE, "google-100", "user@example.com")

                    val loginResult = mockMvc.post("/api/auth/google") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"code":"valid-code"}"""
                    }.andReturn()

                    val accessToken = com.jayway.jsonpath.JsonPath.read<String>(
                        loginResult.response.contentAsString,
                        "$.data.accessToken",
                    )

                    mockMvc.get("/api/users/me") {
                        header("Authorization", "Bearer $accessToken")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.email") { value("user@example.com") }
                        jsonPath("$.data.accountStatus") { value("NOT_CONSENT") }
                        jsonPath("$.data.interests") { isArray() }
                    }
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/users/me")
                        .andExpect {
                            status { isUnauthorized() }
                        }
                }
            }
        }

        describe("GET /api/users/check-nickname") {
            fun loginAndGetToken(): String {
                every { oAuthClient.authenticate("valid-code") } returns
                    OAuthUserInfo(OAuthProvider.GOOGLE, "google-200", "nick@example.com")

                val result = mockMvc.post("/api/auth/google") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"code":"valid-code"}"""
                }.andReturn()

                return com.jayway.jsonpath.JsonPath.read(
                    result.response.contentAsString,
                    "$.data.accessToken",
                )
            }

            context("사용 가능한 닉네임") {
                it("available: true를 반환한다") {
                    val token = loginAndGetToken()

                    mockMvc.get("/api/users/check-nickname?nickname=새닉네임") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.available") { value(true) }
                    }
                }
            }

            context("이미 사용 중인 닉네임") {
                it("available: false를 반환한다") {
                    val token = loginAndGetToken()
                    accountRepository.save(AccountFixture.create(id = 0, nickname = Nickname("중복닉네임")))

                    mockMvc.get("/api/users/check-nickname?nickname=중복닉네임") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.available") { value(false) }
                    }
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/users/check-nickname?nickname=테스트")
                        .andExpect {
                            status { isUnauthorized() }
                        }
                }
            }
        }
    }
}
