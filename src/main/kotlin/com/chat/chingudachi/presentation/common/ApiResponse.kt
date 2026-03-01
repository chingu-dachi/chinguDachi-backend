package com.chat.chingudachi.presentation.common

import com.chat.chingudachi.domain.common.ErrorCode
import java.time.Instant

data class ApiResponse<T>(
    val code: String?,
    val data: T?,
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Instant,
) {
    companion object {
        fun <T> success(
            data: T?,
            message: String = "success",
        ) = ApiResponse(
            code = null,
            data = data,
            message = message,
            isSuccess = true,
            timestamp = Instant.now(),
        )

        fun error(errorCode: ErrorCode) =
            ApiResponse(
                code = errorCode.name,
                data = null,
                message = errorCode.message,
                isSuccess = false,
                timestamp = Instant.now(),
            )

        fun error(
            code: String,
            message: String,
        ) = ApiResponse(
            code = code,
            data = null,
            message = message,
            isSuccess = false,
            timestamp = Instant.now(),
        )
    }
}
