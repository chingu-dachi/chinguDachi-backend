package com.chat.chingudachi.chat.infrastructure.translation.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3,
    @JsonProperty("max_tokens")
    val maxTokens: Int = 500,
)

data class OpenAiMessage(
    val role: String,
    val content: String,
)

data class OpenAiChatResponse(
    val choices: List<Choice>,
) {
    val translatedText: String
        get() = choices.first().message.content.trim()
}

data class Choice(val message: OpenAiMessage)
