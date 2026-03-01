package com.chat.chingudachi.infrastructure.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.google")
data class GoogleOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val allowedRedirectUris: List<String> = emptyList(),
    val tokenUri: String = "https://oauth2.googleapis.com/token",
    val userInfoUri: String = "https://www.googleapis.com/oauth2/v3/userinfo",
)
