package com.chat.chingudachi.infrastructure.translation

import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.InternalServerException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class OpenAiTranslationClientTest : DescribeSpec() {
    private val builder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(builder).build()
    private val restClient = builder.build()

    private val properties = OpenAiProperties(
        apiKey = "test-api-key",
        apiUrl = "https://api.openai.com/v1/chat/completions",
        model = "gpt-4.1-mini",
    )

    private val client = OpenAiTranslationClient(restClient, properties)

    init {
        afterEach { mockServer.reset() }

        describe("translate") {
            val successResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "こんにちは"
                            }
                        }
                    ]
                }
            """.trimIndent()

            context("1순위 모델 성공 시") {
                it("번역 텍스트를 반환한다") {
                    mockServer.expect(requestTo(properties.apiUrl))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(withSuccess(successResponse, MediaType.APPLICATION_JSON))

                    val result = client.translate("안녕하세요", "ko", "ja")

                    result shouldBe "こんにちは"
                    mockServer.verify()
                }
            }

            context("1순위 실패, 2순위 성공 시") {
                it("fallback으로 2순위 모델 결과를 반환한다") {
                    // 1st call fails
                    mockServer.expect(requestTo(properties.apiUrl))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(withServerError())

                    // 2nd call succeeds
                    mockServer.expect(requestTo(properties.apiUrl))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(withSuccess(successResponse, MediaType.APPLICATION_JSON))

                    val result = client.translate("안녕하세요", "ko", "ja")

                    result shouldBe "こんにちは"
                    mockServer.verify()
                }
            }

            context("모든 모델 실패 시") {
                it("TRANSLATION_ALL_MODELS_FAILED 예외를 던진다") {
                    // All 3 calls fail
                    repeat(3) {
                        mockServer.expect(requestTo(properties.apiUrl))
                            .andExpect(method(HttpMethod.POST))
                            .andRespond(withServerError())
                    }

                    val exception = shouldThrow<InternalServerException> {
                        client.translate("안녕하세요", "ko", "ja")
                    }

                    exception.errorCode shouldBe ErrorCode.TRANSLATION_ALL_MODELS_FAILED
                    mockServer.verify()
                }
            }
        }
    }
}
