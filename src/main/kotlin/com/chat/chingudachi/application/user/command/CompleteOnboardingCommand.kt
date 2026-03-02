package com.chat.chingudachi.application.user.command

data class CompleteOnboardingCommand(
    val nickname: String,
    val birthYear: Int,
    val nationality: String,
    val interests: List<String>,
    val bio: String? = null,
    val city: String? = null,
    val profileImageUrl: String? = null,
)
