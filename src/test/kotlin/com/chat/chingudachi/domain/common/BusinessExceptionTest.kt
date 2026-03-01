package com.chat.chingudachi.domain.common

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.http.HttpStatus

class BusinessExceptionTest : DescribeSpec() {
    init {
        describe("BusinessException") {
            context("각 서브클래스 생성 시 ") {
                it("ErrorCode의 message가 exception message로 설정된다.") {
                    val ex = NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

                    ex.message shouldBe "Account not found"
                    ex.errorCode shouldBe ErrorCode.ACCOUNT_NOT_FOUND
                }

                it("cause를 보존한다.") {
                    val cause = RuntimeException("original error")
                    val ex = BadRequestException(ErrorCode.INVALID_INPUT, cause)

                    ex.cause shouldBe cause
                }
            }

            context("sealed class 계층 확인 시 ") {
                it("모든 서브클래스가 BusinessException의 인스턴스다.") {
                    BadRequestException().shouldBeInstanceOf<BusinessException>()
                    UnauthorizedException().shouldBeInstanceOf<BusinessException>()
                    ForbiddenException().shouldBeInstanceOf<BusinessException>()
                    NotFoundException().shouldBeInstanceOf<BusinessException>()
                    ConflictException().shouldBeInstanceOf<BusinessException>()
                    InternalServerException().shouldBeInstanceOf<BusinessException>()
                }

                it("기본 ErrorCode가 올바르게 설정된다.") {
                    BadRequestException().errorCode shouldBe ErrorCode.INVALID_INPUT
                    UnauthorizedException().errorCode shouldBe ErrorCode.UNAUTHORIZED
                    ForbiddenException().errorCode shouldBe ErrorCode.FORBIDDEN
                    NotFoundException().errorCode shouldBe ErrorCode.RESOURCE_NOT_FOUND
                    ConflictException().errorCode shouldBe ErrorCode.CONFLICT
                    InternalServerException().errorCode shouldBe ErrorCode.INTERNAL_ERROR
                }
            }

            context("ErrorCode enum 확인 시 ") {
                it("도메인 에러 코드는 올바른 HTTP 상태를 가진다.") {
                    ErrorCode.ACCOUNT_NOT_FOUND.status shouldBe HttpStatus.NOT_FOUND
                    ErrorCode.ACCOUNT_ALREADY_EXISTS.status shouldBe HttpStatus.CONFLICT
                    ErrorCode.ACCOUNT_NICKNAME_INVALID.status shouldBe HttpStatus.BAD_REQUEST
                    ErrorCode.AUTH_TOKEN_EXPIRED.status shouldBe HttpStatus.UNAUTHORIZED
                    ErrorCode.AUTH_TOKEN_INVALID.status shouldBe HttpStatus.UNAUTHORIZED
                }

                it("name이 에러 코드 식별자로 사용된다.") {
                    ErrorCode.INVALID_INPUT.name shouldBe "INVALID_INPUT"
                    ErrorCode.ACCOUNT_NOT_FOUND.name shouldBe "ACCOUNT_NOT_FOUND"
                    ErrorCode.AUTH_TOKEN_EXPIRED.name shouldBe "AUTH_TOKEN_EXPIRED"
                }
            }
        }
    }
}
