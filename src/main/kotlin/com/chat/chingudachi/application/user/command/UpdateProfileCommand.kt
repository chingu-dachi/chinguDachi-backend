package com.chat.chingudachi.application.user.command

data class UpdateProfileCommand(
    val nickname: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val interests: List<String>? = null,
    val profileImageUrl: String? = null,
)
