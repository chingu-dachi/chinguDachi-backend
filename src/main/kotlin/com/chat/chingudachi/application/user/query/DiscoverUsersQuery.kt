package com.chat.chingudachi.application.user.query

import com.chat.chingudachi.application.common.PaginationConstants
import com.chat.chingudachi.domain.account.Nation

data class DiscoverUsersQuery(
    val currentUserId: Long,
    val nationality: Nation? = null,
    val interests: List<String>? = null,
    val cursor: Long? = null,
    val size: Int = PaginationConstants.DEFAULT_SIZE,
)
