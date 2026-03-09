package com.chat.chingudachi.core.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class AppCorsConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun corsConfigurationSource() =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOrigins = corsProperties.allowedOrigins
                    allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
                    allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "x-auth-retry")
                    allowCredentials = true
                },
            )
        }
}

@ConfigurationProperties("cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
)
