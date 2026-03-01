package com.chat.chingudachi.presentation.auth.dto

data class LoginResponse(
    val accessToken: String,
    val onboardingRequired: Boolean,
)
