package com.chat.chingudachi.presentation.common

import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.BusinessException
import com.chat.chingudachi.domain.common.ConflictException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import com.chat.chingudachi.domain.common.InternalServerException
import com.chat.chingudachi.domain.common.NotFoundException
import com.chat.chingudachi.domain.common.UnauthorizedException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

private val log = KotlinLogging.logger {}

@RestControllerAdvice(basePackages = ["com.chat.chingudachi"])
class ErrorResponseWrappingAdvice {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val status = resolveStatus(ex)
        if (status.is5xxServerError) {
            log.error(ex) { "Business exception: ${ex.errorCode.name} - ${ex.message}" }
        } else {
            log.warn { "Business exception: ${ex.errorCode.name} - ${ex.message}" }
        }
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.errorCode))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val detail =
            ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn { "Validation failed: $detail" }
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name, detail))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn { "Illegal argument: ${ex.message}" }
        return ResponseEntity
            .badRequest()
            .body(
                ApiResponse.error(
                    ErrorCode.INVALID_INPUT.name,
                    ex.message ?: ErrorCode.INVALID_INPUT.message,
                ),
            )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn { "Method not allowed: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn { "Resource not found: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.warn { "Data integrity violation: ${ex.rootCause?.message}" }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorCode.CONFLICT))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.error(ex) { "Unhandled exception [${request.method}] ${request.requestURI}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR))
    }

    private fun resolveStatus(ex: BusinessException): HttpStatus = when (ex) {
        is BadRequestException -> HttpStatus.BAD_REQUEST
        is UnauthorizedException -> HttpStatus.UNAUTHORIZED
        is ForbiddenException -> HttpStatus.FORBIDDEN
        is NotFoundException -> HttpStatus.NOT_FOUND
        is ConflictException -> HttpStatus.CONFLICT
        is InternalServerException -> HttpStatus.INTERNAL_SERVER_ERROR
    }
}
