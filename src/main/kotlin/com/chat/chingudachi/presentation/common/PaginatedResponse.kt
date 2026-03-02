package com.chat.chingudachi.presentation.common

data class PaginatedResponse<T>(
    val items: List<T>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
