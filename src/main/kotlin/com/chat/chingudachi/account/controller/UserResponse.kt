package com.chat.chingudachi.account.controller

import com.chat.chingudachi.account.service.UserPublicProfile
import java.time.Instant

data class UserResponse(
    val userId: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val nationality: String?,
    val translateLanguage: String?,
    val birthYear: Int?,
    val interests: List<String>,
    val bio: String?,
    val city: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(profile: UserPublicProfile) = UserResponse(
            userId = profile.userId.toString(),
            nickname = profile.nickname,
            profileImageUrl = profile.profileImageUrl,
            nationality = profile.nationality,
            translateLanguage = profile.translateLanguage,
            birthYear = profile.birthYear,
            interests = profile.interests,
            bio = profile.bio,
            city = profile.city,
            createdAt = profile.createdAt,
        )
    }
}
