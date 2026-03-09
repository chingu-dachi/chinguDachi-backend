package com.chat.chingudachi.auth.application.result

data class AuthenticateResult(
    val accessToken: String,
    val refreshToken: String,
    val onboardingRequired: Boolean,
    val consentRequired: Boolean,
)
