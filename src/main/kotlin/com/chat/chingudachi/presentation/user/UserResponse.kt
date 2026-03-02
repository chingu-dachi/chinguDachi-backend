package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.interest.UserInterest
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
        fun from(account: Account, interests: List<UserInterest>) = UserResponse(
            userId = account.id.toString(),
            nickname = account.nickname?.value,
            profileImageUrl = account.profileImageUrl,
            nationality = account.nation?.name,
            translateLanguage = account.translateLanguage?.name?.lowercase(),
            birthYear = account.birthDate?.year,
            interests = interests.map { it.interestTag.tagKey },
            bio = account.bio,
            city = account.city,
            createdAt = account.createdAt,
        )
    }
}
