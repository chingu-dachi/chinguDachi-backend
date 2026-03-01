package com.chat.chingudachi.domain.auth

data class OAuthUserInfo(
    val provider: String,
    val sub: String,
    val email: String,
)
