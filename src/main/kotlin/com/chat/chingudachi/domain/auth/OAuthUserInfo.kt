package com.chat.chingudachi.domain.auth

data class OAuthUserInfo(
    val provider: OAuthProvider,
    val providerUserId: String,
    val email: String,
)
