package com.chat.chingudachi.auth.domain

data class OAuthUserInfo(
    val provider: OAuthProvider,
    val providerUserId: String,
    val email: String,
)
