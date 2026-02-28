package com.chat.chingudachi.presentation.common

import java.time.Instant

data class ApiResponse<T>(
    val data: T?,
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Instant,
    val status: Int,
) {
    companion object {
        fun <T> success(data: T?, message: String = "success", status: Int = 200) = ApiResponse(
            data = data,
            message = message,
            isSuccess = true,
            timestamp = Instant.now(),
            status = status,
        )

        fun error(message: String, status: Int) = ApiResponse<Nothing>(
            data = null,
            message = message,
            isSuccess = false,
            timestamp = Instant.now(),
            status = status,
        )
    }
}
