package com.chat.chingudachi.core.infrastructure.config

import com.chat.chingudachi.chat.infrastructure.translation.OpenAiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class RestClientConfig {
    @Bean("googleRestClient")
    fun googleRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(Duration.ofSeconds(5))
        factory.setReadTimeout(Duration.ofSeconds(10))
        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }

    @Bean("openaiRestClient")
    fun openaiRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(Duration.ofSeconds(2))
        factory.setReadTimeout(Duration.ofSeconds(6))
        return RestClient.builder()
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
