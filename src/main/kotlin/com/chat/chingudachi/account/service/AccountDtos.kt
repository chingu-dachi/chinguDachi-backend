package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.core.application.PaginationConstants
import com.chat.chingudachi.interest.domain.UserInterest
import java.time.Instant

data class AgreeToTermsCommand(
    val termsOfServiceAgree: Boolean,
    val privacyPolicyAgree: Boolean,
    val crossBorderDataAgree: Boolean,
    val marketingAgree: Boolean,
)

data class CompleteOnboardingCommand(
    val nickname: String,
    val birthYear: Int,
    val nationality: String,
    val interests: List<String>,
    val bio: String? = null,
    val city: String? = null,
    val profileImageUrl: String? = null,
)

data class UpdateProfileCommand(
    val nickname: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val interests: List<String>? = null,
    val profileImageUrl: String? = null,
)

data class DiscoverUsersQuery(
    val currentUserId: Long,
    val nationality: Nation? = null,
    val interests: List<String>? = null,
    val cursor: Long? = null,
    val size: Int = PaginationConstants.DEFAULT_SIZE,
)

data class MyProfile(
    val account: Account,
    val interests: List<UserInterest>,
)

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

data class DiscoverUsersResult(
    val items: List<UserPublicProfile>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
