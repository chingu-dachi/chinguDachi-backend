package com.chat.chingudachi.infrastructure.security

import com.chat.chingudachi.application.auth.port.TokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.slf4j.MDC
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : DescribeSpec() {
    private val tokenProvider = mockk<TokenProvider>()
    private val filter = JwtAuthenticationFilter(tokenProvider)

    init {
        afterEach {
            SecurityContextHolder.clearContext()
            clearMocks(tokenProvider)
        }

        describe("JwtAuthenticationFilter") {
            context("유효한 JWT가 Authorization 헤더에 있는 경우") {
                it("SecurityContext에 accountId를 설정한다") {
                    val request = MockHttpServletRequest("GET", "/api/users/me")
                    request.addHeader("Authorization", "Bearer valid-token")
                    val response = MockHttpServletResponse()

                    every { tokenProvider.validateToken("valid-token") } returns true
                    every { tokenProvider.parseAccountId("valid-token") } returns 42L

                    filter.doFilter(request, response, MockFilterChain())

                    val auth = SecurityContextHolder.getContext().authentication!!
                    auth.principal shouldBe 42L
                    auth.isAuthenticated shouldBe true
                }

                it("필터 완료 후 MDC userId가 정리된다") {
                    val request = MockHttpServletRequest("GET", "/api/users/me")
                    request.addHeader("Authorization", "Bearer valid-token")
                    val response = MockHttpServletResponse()

                    every { tokenProvider.validateToken("valid-token") } returns true
                    every { tokenProvider.parseAccountId("valid-token") } returns 42L

                    filter.doFilter(request, response, MockFilterChain())

                    MDC.get("userId").shouldBeNull()
                }
            }

            context("Authorization 헤더가 없는 경우") {
                it("SecurityContext를 설정하지 않고 필터를 통과시킨다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()

                    filter.doFilter(request, response, MockFilterChain())

                    SecurityContextHolder.getContext().authentication.shouldBeNull()
                }
            }

            context("Bearer 프리픽스가 없는 경우") {
                it("SecurityContext를 설정하지 않는다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    request.addHeader("Authorization", "Basic some-token")
                    val response = MockHttpServletResponse()

                    filter.doFilter(request, response, MockFilterChain())

                    SecurityContextHolder.getContext().authentication.shouldBeNull()
                }
            }

            context("유효하지 않은 JWT인 경우") {
                it("SecurityContext를 설정하지 않고 필터를 통과시킨다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    request.addHeader("Authorization", "Bearer invalid-token")
                    val response = MockHttpServletResponse()

                    every { tokenProvider.validateToken("invalid-token") } returns false

                    filter.doFilter(request, response, MockFilterChain())

                    SecurityContextHolder.getContext().authentication.shouldBeNull()
                }
            }
        }
    }
}
