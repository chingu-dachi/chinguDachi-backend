package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.domain.account.Nation
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
import org.springframework.test.web.servlet.put
import java.time.LocalDate

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

    private fun onboardingRequest(
        nickname: String = "테스트유저",
        birthDate: LocalDate = LocalDate.of(2000, 1, 1),
        nation: Nation = Nation.KR,
        interestTagIds: List<Long> = emptyList(),
    ): String = objectMapper.writeValueAsString(
        CompleteOnboardingRequest(
            nickname = nickname,
            birthDate = birthDate,
            nation = nation,
            interestTagIds = interestTagIds,
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
                        content = onboardingRequest(
                            nickname = "테스트유저",
                            interestTagIds = listOf(tags[0].id, tags[1].id),
                        )
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

            context("이미 온보딩 완료된 계정이면") {
                it("409를 반환한다") {
                    val token = loginAndGetToken()
                    val tags = seedInterestTags()

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "첫번째닉",
                            interestTagIds = listOf(tags[0].id),
                        )
                    }.andExpect {
                        status { isOk() }
                    }

                    mockMvc.put("/api/users/profile") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(
                            nickname = "두번째닉",
                            interestTagIds = listOf(tags[1].id),
                        )
                    }.andExpect {
                        status { isConflict() }
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
                        content = onboardingRequest(
                            nickname = "중복닉네임",
                            interestTagIds = listOf(tags[0].id),
                        )
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
                        content = onboardingRequest(interestTagIds = emptyList())
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
                        content = onboardingRequest(interestTagIds = listOf(9999L))
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.put("/api/users/profile") {
                        contentType = MediaType.APPLICATION_JSON
                        content = onboardingRequest(interestTagIds = listOf(1L))
                    }.andExpect {
                        status { isUnauthorized() }
                    }
                }
            }
        }
    }
}
