package com.chat.chingudachi.auth.infra.security

import com.chat.chingudachi.auth.infra.TokenProvider
import com.chat.chingudachi.core.domain.ErrorCode
import jakarta.servlet.http.HttpServletResponse
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
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Configuration
@EnableWebSecurity
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
                authorize(HttpMethod.POST, "/api/auth/google", permitAll)
                authorize(HttpMethod.POST, "/api/auth/refresh", permitAll)
                authorize(HttpMethod.GET, "/api/interest-tags", permitAll)
                authorize(HttpMethod.GET, "/api/users/check-nickname", permitAll)
                authorize("/ws/**", permitAll)
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
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
