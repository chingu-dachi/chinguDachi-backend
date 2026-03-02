package com.chat.chingudachi.infrastructure.config

import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.infrastructure.security.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig(
    private val tokenProvider: TokenProvider,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { }
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/api/auth/**", permitAll)
                authorize(HttpMethod.GET, "/api/interest-tags", permitAll)
                authorize(HttpMethod.GET, "/api/users/check-nickname", permitAll)
                authorize(HttpMethod.GET, "/uploads/**", permitAll)
                authorize("/error", permitAll)
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = authenticationEntryPoint()
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(JwtAuthenticationFilter(tokenProvider))
        }
        return http.build()
    }

    private fun authenticationEntryPoint() =
        org.springframework.security.web.AuthenticationEntryPoint { _, response, _ ->
            val errorCode = ErrorCode.UNAUTHORIZED
            val errorBody =
                mapOf(
                    "code" to errorCode.name,
                    "data" to null,
                    "message" to errorCode.message,
                    "isSuccess" to false,
                    "timestamp" to Instant.now(),
                )
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(response.outputStream, errorBody)
        }
}

@Configuration
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
