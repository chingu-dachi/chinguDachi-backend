package com.chat.chingudachi.infrastructure.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiry: Duration,
    val refreshTokenExpiry: Duration,
)
