package com.chat.chingudachi.chat.infrastructure.translation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("openai")
data class OpenAiProperties(
    val apiKey: String,
    val apiUrl: String = "https://api.openai.com/v1/chat/completions",
    val model: String = "gpt-4.1-mini",
)
