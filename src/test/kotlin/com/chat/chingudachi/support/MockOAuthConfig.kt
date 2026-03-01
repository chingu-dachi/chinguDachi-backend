package com.chat.chingudachi.support

import com.chat.chingudachi.application.auth.port.OAuthClient
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class MockOAuthConfig {
    @Bean
    @Primary
    fun mockOAuthClient(): OAuthClient = mockk()
}
