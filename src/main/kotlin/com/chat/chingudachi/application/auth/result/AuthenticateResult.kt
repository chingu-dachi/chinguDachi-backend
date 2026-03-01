package com.chat.chingudachi.application.auth.result

data class AuthenticateResult(
    val accessToken: String,
    val refreshToken: String,
    val onboardingRequired: Boolean,
)
