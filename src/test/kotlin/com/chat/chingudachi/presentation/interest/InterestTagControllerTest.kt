package com.chat.chingudachi.presentation.interest

import com.chat.chingudachi.domain.interest.InterestTag
import com.chat.chingudachi.infrastructure.persistence.interest.InterestTagRepository
import com.chat.chingudachi.support.MockOAuthConfig
import io.kotest.core.spec.style.DescribeSpec
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Import(MockOAuthConfig::class)
class InterestTagControllerTest(
    private val mockMvc: MockMvc,
    private val interestTagRepository: InterestTagRepository,
) : DescribeSpec() {

    init {
        afterEach {
            interestTagRepository.deleteAll()
        }

        describe("GET /api/interest-tags") {
            context("태그가 존재하면") {
                it("displayOrder 순으로 반환한다") {
                    interestTagRepository.saveAll(
                        listOf(
                            InterestTag(tagKey = "MUSIC", labelKo = "음악", labelJa = "音楽", displayOrder = 3),
                            InterestTag(tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
                            InterestTag(tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
                        ),
                    )

                    mockMvc.get("/api/interest-tags")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.data.length()") { value(3) }
                            jsonPath("$.data[0].tagKey") { value("TRAVEL") }
                            jsonPath("$.data[0].displayOrder") { value(1) }
                            jsonPath("$.data[1].tagKey") { value("FOOD") }
                            jsonPath("$.data[2].tagKey") { value("MUSIC") }
                            header { string("Cache-Control", "max-age=3600, public") }
                        }
                }
            }

            context("태그가 없으면") {
                it("빈 배열을 반환한다") {
                    mockMvc.get("/api/interest-tags")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.data") { isArray() }
                            jsonPath("$.data.length()") { value(0) }
                        }
                }
            }

            context("인증 없이도") {
                it("접근 가능하다") {
                    mockMvc.get("/api/interest-tags")
                        .andExpect {
                            status { isOk() }
                        }
                }
            }
        }
    }
}
