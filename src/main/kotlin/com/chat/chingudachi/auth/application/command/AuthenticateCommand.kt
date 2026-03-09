package com.chat.chingudachi.auth.application.command

data class AuthenticateCommand(
    val code: String,
    val redirectUri: String? = null,
)
