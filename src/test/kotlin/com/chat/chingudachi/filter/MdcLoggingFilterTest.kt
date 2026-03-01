package com.chat.chingudachi.filter

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class MdcLoggingFilterTest : DescribeSpec() {
    private val filter = MdcLoggingFilter()

    private fun capturingChain(block: () -> Unit) =
        FilterChain { _: ServletRequest, _: ServletResponse -> block() }

    init {
        afterEach { MDC.clear() }

        describe("MdcLoggingFilter") {
            context("요청 처리 시") {
                it("MDC에 requestId를 8자리 UUID로 설정한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()
                    var capturedRequestId: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedRequestId = MDC.get(MdcLoggingFilter.REQUEST_ID)
                    })

                    capturedRequestId shouldHaveLength 8
                    capturedRequestId!! shouldMatch "[0-9a-f]{8}"
                }

                it("MDC에 method, uri를 설정한다") {
                    val request = MockHttpServletRequest("POST", "/api/accounts")
                    val response = MockHttpServletResponse()
                    var capturedMethod: String? = null
                    var capturedUri: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedMethod = MDC.get(MdcLoggingFilter.METHOD)
                        capturedUri = MDC.get(MdcLoggingFilter.URI)
                    })

                    capturedMethod shouldBe "POST"
                    capturedUri shouldBe "/api/accounts"
                }

                it("MDC에 userId를 anonymous로 설정한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()
                    var capturedUserId: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedUserId = MDC.get(MdcLoggingFilter.USER_ID)
                    })

                    capturedUserId shouldBe "anonymous"
                }
            }

            context("클라이언트 IP 추출 시") {
                it("X-Forwarded-For 헤더가 있으면 첫 번째 IP를 사용한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18")
                    val response = MockHttpServletResponse()
                    var capturedIp: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedIp = MDC.get(MdcLoggingFilter.CLIENT_IP)
                    })

                    capturedIp shouldBe "203.0.113.50"
                }

                it("X-Forwarded-For가 없으면 remoteAddr를 사용한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    request.remoteAddr = "127.0.0.1"
                    val response = MockHttpServletResponse()
                    var capturedIp: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedIp = MDC.get(MdcLoggingFilter.CLIENT_IP)
                    })

                    capturedIp shouldBe "127.0.0.1"
                }
            }

            context("User-Agent, Origin 헤더 처리 시") {
                it("헤더가 있으면 MDC에 설정한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    request.addHeader("User-Agent", "Mozilla/5.0")
                    request.addHeader("Origin", "http://localhost:3000")
                    val response = MockHttpServletResponse()
                    var capturedUserAgent: String? = null
                    var capturedOrigin: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedUserAgent = MDC.get(MdcLoggingFilter.USER_AGENT)
                        capturedOrigin = MDC.get(MdcLoggingFilter.ORIGIN)
                    })

                    capturedUserAgent shouldBe "Mozilla/5.0"
                    capturedOrigin shouldBe "http://localhost:3000"
                }

                it("헤더가 없으면 빈 문자열로 설정한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()
                    var capturedUserAgent: String? = null
                    var capturedOrigin: String? = null

                    filter.doFilter(request, response, capturingChain {
                        capturedUserAgent = MDC.get(MdcLoggingFilter.USER_AGENT)
                        capturedOrigin = MDC.get(MdcLoggingFilter.ORIGIN)
                    })

                    capturedUserAgent shouldBe ""
                    capturedOrigin shouldBe ""
                }
            }

            context("필터 완료 후") {
                it("MDC를 클리어한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()

                    filter.doFilter(request, response, MockFilterChain())

                    MDC.get(MdcLoggingFilter.REQUEST_ID).shouldBeNull()
                    MDC.get(MdcLoggingFilter.METHOD).shouldBeNull()
                    MDC.get(MdcLoggingFilter.URI).shouldBeNull()
                    MDC.get(MdcLoggingFilter.CLIENT_IP).shouldBeNull()
                    MDC.get(MdcLoggingFilter.USER_ID).shouldBeNull()
                }

                it("예외 발생 시에도 MDC를 클리어한다") {
                    val request = MockHttpServletRequest("GET", "/api/test")
                    val response = MockHttpServletResponse()
                    val chain = capturingChain { throw RuntimeException("test error") }

                    try {
                        filter.doFilter(request, response, chain)
                    } catch (_: RuntimeException) {
                        // expected
                    }

                    MDC.get(MdcLoggingFilter.REQUEST_ID).shouldBeNull()
                }
            }
        }
    }
}
