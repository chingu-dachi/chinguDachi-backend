package com.chat.chingudachi.infrastructure.oauth

import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.InternalServerException
import com.chat.chingudachi.domain.common.UnauthorizedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class GoogleOAuthClientTest : DescribeSpec() {
    private val builder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(builder).build()
    private val restClient = builder.build()

    private val properties = GoogleOAuthProperties(
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        redirectUri = "http://localhost:3000/callback",
    )

    private val googleOAuthClient = GoogleOAuthClient(restClient, properties)

    init {
        afterEach { mockServer.reset() }

        describe("authenticate") {
            context("정상 흐름") {
                it("code → token 교환 → userInfo 조회 → OAuthUserInfo를 반환한다") {
                    mockServer.expect(requestTo(properties.tokenUri))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(
                            withSuccess(
                                """{"access_token":"google-access-token","token_type":"Bearer","expires_in":3600}""",
                                MediaType.APPLICATION_JSON,
                            ),
                        )

                    mockServer.expect(requestTo(properties.userInfoUri))
                        .andExpect(method(HttpMethod.GET))
                        .andRespond(
                            withSuccess(
                                """{"sub":"google-user-123","email":"test@gmail.com"}""",
                                MediaType.APPLICATION_JSON,
                            ),
                        )

                    val result = googleOAuthClient.authenticate("valid-code")

                    result.provider shouldBe OAuthProvider.GOOGLE
                    result.providerUserId shouldBe "google-user-123"
                    result.email shouldBe "test@gmail.com"

                    mockServer.verify()
                }
            }

            context("토큰 교환 실패 시") {
                it("AUTH_OAUTH_CODE_INVALID 예외를 던진다") {
                    mockServer.expect(requestTo(properties.tokenUri))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(withBadRequest())

                    val exception = shouldThrow<UnauthorizedException> {
                        googleOAuthClient.authenticate("invalid-code")
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_OAUTH_CODE_INVALID
                }
            }

            context("userInfo 조회 실패 시") {
                it("AUTH_OAUTH_PROVIDER_ERROR 예외를 던진다") {
                    mockServer.expect(requestTo(properties.tokenUri))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(
                            withSuccess(
                                """{"access_token":"google-access-token","token_type":"Bearer","expires_in":3600}""",
                                MediaType.APPLICATION_JSON,
                            ),
                        )

                    mockServer.expect(requestTo(properties.userInfoUri))
                        .andExpect(method(HttpMethod.GET))
                        .andRespond(withServerError())

                    val exception = shouldThrow<InternalServerException> {
                        googleOAuthClient.authenticate("valid-code")
                    }

                    exception.errorCode shouldBe ErrorCode.AUTH_OAUTH_PROVIDER_ERROR
                }
            }
        }
    }
}
