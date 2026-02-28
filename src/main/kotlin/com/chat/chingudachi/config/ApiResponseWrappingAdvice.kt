package com.chat.chingudachi.config

import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.time.Instant

@RestControllerAdvice
class ApiResponseWrappingAdvice : ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = returnType.parameterType != ApiResponse::class.java

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body is ApiResponse) return body
        return ApiResponse(body)
    }
}

@RestControllerAdvice
class ErrorResponseWrappingAdvice {
    @ExceptionHandler(Exception::class)
    fun handleThrowable(ex: Exception): ResponseEntity<ApiResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponse(
                    data = null,
                    message = ex.message ?: "System Error Has been occurred",
                    isSuccess = false,
                    timestamp = Instant.now(),
                    status = 500,
                ),
            )
}

data class ApiResponse(
    val data: Any?,
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Instant,
    val status: Int,
) {
    constructor(data: Any?) : this(
        data = data,
        message = "success",
        isSuccess = true,
        timestamp = Instant.now(),
        status = 200,
    )
}
