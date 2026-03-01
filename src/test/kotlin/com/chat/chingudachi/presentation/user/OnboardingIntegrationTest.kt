package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import com.chat.chingudachi.domain.interest.InterestTag
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.infrastructure.persistence.account.AccountCredentialRepository
import com.chat.chingudachi.infrastructure.persistence.account.AccountRepository
import com.chat.chingudachi.infrastructure.persistence.auth.AuthTokenRepository
import com.chat.chingudachi.infrastructure.persistence.interest.InterestTagRepository
import com.chat.chingudachi.infrastructure.persistence.interest.UserInterestRepository
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
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Import(MockOAuthConfig::class)
class OnboardingIntegrationTest(
    private val mockMvc: MockMvc,
    private val oAuthClient: OAuthClient,
    private val authTokenRepository: AuthTokenRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val accountRepository: AccountRepository,
    private val interestTagRepository: InterestTagRepository,
    private val userInterestRepository: UserInterestRepository,
) : DescribeSpec() {

    private fun loginAndGetToken(oauthKey: String = "google-300", email: String = "onboard@example.com"): String {
        every { oAuthClient.authenticate("valid-code") } returns
            OAuthUserInfo(OAuthProvider.GOOGLE, oauthKey, email)

        val result = mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"valid-code"}"""
        }.andReturn()

        return com.jayway.jsonpath.JsonPath.read(
            result.response.contentAsString,
            "$.data.accessToken",
        )
    }

    private fun seedInterestTags(): List<InterestTag> =
        interestTagRepository.saveAll(
            listOf(
                InterestTag(tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
                InterestTag(tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
                InterestTag(tagKey = "MUSIC", labelKo = "음악", labelJa = "音楽", displayOrder = 3),
            ),
        )

    init {
        afterEach {
            clearMocks(oAuthClient)
            userInterestRepository.deleteAll()
            authTokenRepository.deleteAll()
            accountCredentialRepository.deleteAll()
            accountRepository.deleteAll()
            interestTagRepository.deleteAll()
        }

        describe("PUT /api/users/profile") {
            context("유효한 온보딩 데이터") {
                it("계정 상태를 ACTIVE로 변경한다") {
                    val token = loginAndGetToken()
                    val tags = seedInterestTags()

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = """
                            {
                                "nickname": "테스트유저",
                                "birthDate": "2000-01-01",
                                "nation": "KR",
                                "interestTagIds": [${tags[0].id}, ${tags[1].id}]
                            }
                        """.trimIndent()
                    }.andExpect {
                        status { isOk() }
                    }

                    mockMvc.get("/api/users/me") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.accountStatus") { value("ACTIVE") }
                        jsonPath("$.data.nickname") { value("테스트유저") }
                        jsonPath("$.data.nation") { value("KR") }
                        jsonPath("$.data.nativeLanguage") { value("KO") }
                        jsonPath("$.data.interests") { isArray() }
                    }
                }
            }

            context("닉네임이 중복되면") {
                it("409를 반환한다") {
                    val token = loginAndGetToken()
                    val tags = seedInterestTags()
                    accountRepository.save(AccountFixture.create(id = 0, nickname = Nickname("중복닉네임")))

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = """
                            {
                                "nickname": "중복닉네임",
                                "birthDate": "2000-01-01",
                                "nation": "KR",
                                "interestTagIds": [${tags[0].id}]
                            }
                        """.trimIndent()
                    }.andExpect {
                        status { isConflict() }
                    }
                }
            }

            context("관심사를 선택하지 않으면") {
                it("400을 반환한다") {
                    val token = loginAndGetToken()

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = """
                            {
                                "nickname": "테스트유저",
                                "birthDate": "2000-01-01",
                                "nation": "KR",
                                "interestTagIds": []
                            }
                        """.trimIndent()
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            context("존재하지 않는 관심사 태그 ID") {
                it("400을 반환한다") {
                    val token = loginAndGetToken()

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = """
                            {
                                "nickname": "테스트유저",
                                "birthDate": "2000-01-01",
                                "nation": "KR",
                                "interestTagIds": [9999]
                            }
                        """.trimIndent()
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.put("/api/users/profile") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """
                            {
                                "nickname": "테스트유저",
                                "birthDate": "2000-01-01",
                                "nation": "KR",
                                "interestTagIds": [1]
                            }
                        """.trimIndent()
                    }.andExpect {
                        status { isUnauthorized() }
                    }
                }
            }
        }
    }
}
