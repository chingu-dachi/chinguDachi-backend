package com.chat.chingudachi.auth.infra.oauth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class OAuthRestClientConfig {
    @Bean("googleRestClient")
    fun googleRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(Duration.ofSeconds(5))
        factory.setReadTimeout(Duration.ofSeconds(10))
        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }
}
