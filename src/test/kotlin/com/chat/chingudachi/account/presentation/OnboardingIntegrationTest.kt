package com.chat.chingudachi.account.presentation

import com.chat.chingudachi.auth.application.port.OAuthClient
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo
import com.chat.chingudachi.interest.domain.InterestTag
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.auth.infrastructure.persistence.AccountCredentialRepository
import com.chat.chingudachi.account.infrastructure.persistence.AccountRepository
import com.chat.chingudachi.account.infrastructure.persistence.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.infrastructure.persistence.AuthTokenRepository
import com.chat.chingudachi.interest.infrastructure.persistence.InterestTagRepository
import com.chat.chingudachi.interest.infrastructure.persistence.UserInterestRepository
import com.chat.chingudachi.support.MockOAuthConfig
import tools.jackson.databind.ObjectMapper
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
class OnboardingIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val oAuthClient: OAuthClient,
    private val authTokenRepository: AuthTokenRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val accountRepository: AccountRepository,
    private val interestTagRepository: InterestTagRepository,
    private val userInterestRepository: UserInterestRepository,
    private val termsAgreementRepository: AccountTermsAgreementRepository,
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

    private fun agreeToTerms(token: String) {
        mockMvc.post("/api/users/consent") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"termsOfServiceAgree":true,"privacyPolicyAgree":true,"crossBorderDataAgree":true}"""
        }.andExpect { status { isNoContent() } }
    }

    private fun onboardingRequest(
        nickname: String = "테스트유저",
        birthYear: Int = 2000,
        nationality: String = "KR",
        interests: List<String> = emptyList(),
    ): String = objectMapper.writeValueAsString(
        CompleteOnboardingRequest(
            nickname = nickname,
            birthYear = birthYear,
            nationality = nationality,
            interests = interests,
        ),
    )

    init {
        afterEach {
            clearMocks(oAuthClient)
            userInterestRepository.deleteAll()
            termsAgreementRepository.deleteAll()
            authTokenRepository.deleteAll()
            accountCredentialRepository.deleteAll()
            accountRepository.deleteAll()
            interestTagRepository.deleteAll()
        }

        describe("POST /api/users/me/onboarding") {
            context("유효한 온보딩 데이터") {
                it("계정 상태를 ACTIVE로 변경한다") {
                    val token = loginAndGetToken()
                    seedInterestTags()
                    agreeToTerms(token)

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "테스트유저",
                            interests = listOf("TRAVEL", "FOOD"),
                        )
                    }.andExpect {
                        status { isOk() }
                    }

                    mockMvc.get("/api/users/me") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.onboardingRequired") { value(false) }
                        jsonPath("$.data.nickname") { value("테스트유저") }
                        jsonPath("$.data.nationality") { value("KR") }
                        jsonPath("$.data.translateLanguage") { value("ja") }
                        jsonPath("$.data.interests") { isArray() }
                    }
                }
            }

            context("이미 온보딩 완료된 계정이면") {
                it("409를 반환한다") {
                    val token = loginAndGetToken()
                    seedInterestTags()
                    agreeToTerms(token)

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "첫번째닉",
                            interests = listOf("TRAVEL"),
                        )
                    }.andExpect {
                        status { isOk() }
                    }

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "두번째닉",
                            interests = listOf("FOOD"),
                        )
                    }.andExpect {
                        status { isConflict() }
                    }
                }
            }

            context("닉네임이 중복되면") {
                it("409를 반환한다") {
                    val token = loginAndGetToken()
                    seedInterestTags()
                    agreeToTerms(token)
                    accountRepository.save(AccountFixture.create(id = 0, nickname = Nickname("중복닉네임")))

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "중복닉네임",
                            interests = listOf("TRAVEL"),
                        )
                    }.andExpect {
                        status { isConflict() }
                    }
                }
            }

            context("관심사를 선택하지 않으면") {
                it("400을 반환한다") {
                    val token = loginAndGetToken()

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(interests = emptyList())
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            context("존재하지 않는 관심사 태그 키") {
                it("400을 반환한다") {
                    val token = loginAndGetToken()
                    agreeToTerms(token)

                    mockMvc.post("/api/users/me/onboarding") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(interests = listOf("INVALID_TAG"))
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.post("/api/users/me/onboarding") {
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(interests = listOf("TRAVEL"))
                    }.andExpect {
                        status { isUnauthorized() }
                    }
                }
            }
        }
    }
}
