package com.chat.chingudachi.account.application.query

import com.chat.chingudachi.core.application.PaginationConstants
import com.chat.chingudachi.account.domain.Nation

data class DiscoverUsersQuery(
    val currentUserId: Long,
    val nationality: Nation? = null,
    val interests: List<String>? = null,
    val cursor: Long? = null,
    val size: Int = PaginationConstants.DEFAULT_SIZE,
)
