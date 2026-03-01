package com.chat.chingudachi.presentation.common

import com.chat.chingudachi.domain.common.BusinessException
import com.chat.chingudachi.domain.common.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice(basePackages = ["com.chat.chingudachi"])
class ErrorResponseWrappingAdvice {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        if (ex.errorCode.status.is5xxServerError) {
            log.error("Business exception: {} - {}", ex.errorCode.name, ex.message, ex)
        } else {
            log.warn("Business exception: {} - {}", ex.errorCode.name, ex.message)
        }
        return ResponseEntity
            .status(ex.errorCode.status)
            .body(ApiResponse.error(ex.errorCode))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val detail =
            ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Validation failed: {}", detail)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name, detail))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Illegal argument: {}", ex.message)
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
        log.warn("Method not allowed: {}", ex.message)
        return ResponseEntity
            .status(ErrorCode.METHOD_NOT_ALLOWED.status)
            .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Resource not found: {}", ex.message)
        return ResponseEntity
            .status(ErrorCode.RESOURCE_NOT_FOUND.status)
            .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception [{}] {}", request.method, request.requestURI, ex)
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.status)
            .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR))
    }
}
