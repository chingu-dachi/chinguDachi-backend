package com.chat.chingudachi.presentation.common

import java.time.Instant

data class ApiResponse<T>(
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
            data = data,
            message = message,
            isSuccess = true,
            timestamp = Instant.now(),
        )

        fun error(message: String) =
            ApiResponse(
                data = null,
                message = message,
                isSuccess = false,
                timestamp = Instant.now(),
            )
    }
}
