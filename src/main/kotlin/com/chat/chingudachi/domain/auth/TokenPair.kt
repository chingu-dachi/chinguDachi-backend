package com.chat.chingudachi.domain.auth

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
