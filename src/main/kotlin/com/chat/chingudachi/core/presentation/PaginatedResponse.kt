package com.chat.chingudachi.core.presentation

data class PaginatedResponse<T>(
    val items: List<T>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
