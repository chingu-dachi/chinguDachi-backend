package com.chat.chingudachi.presentation.common

data class PaginatedResponse<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
