package com.chat.chingudachi.auth.domain

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
