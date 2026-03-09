package com.chat.chingudachi.account.application

import java.time.Instant

data class UserPublicProfile(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
    val nationality: String?,
    val translateLanguage: String?,
    val birthYear: Int?,
    val interests: List<String>,
    val bio: String?,
    val city: String?,
    val createdAt: Instant,
)
