package com.chat.chingudachi.infrastructure.translation

import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.InternalServerException
import com.chat.chingudachi.infrastructure.translation.dto.OpenAiChatRequest
import com.chat.chingudachi.infrastructure.translation.dto.OpenAiChatResponse
import com.chat.chingudachi.infrastructure.translation.dto.OpenAiMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val log = KotlinLogging.logger {}

@Component
class OpenAiTranslationClient(
    @Qualifier("openaiRestClient") private val restClient: RestClient,
    private val properties: OpenAiProperties,
) : TranslationPort {

    companion object {
        private const val SYSTEM_PROMPT = """당신은 한국어↔일본어 채팅 번역가입니다.
자연스러운 구어체로 번역하세요.
존댓말/반말은 원문의 어조를 유지하세요.
이모티콘과 줄임말은 해당 언어의 동등한 표현으로 변환하세요.
번역문만 출력하세요. 설명이나 부가 텍스트를 추가하지 마세요."""
    }

    private val fallbackModels: List<String> by lazy {
        listOf(
            properties.model,
            "gpt-4o-mini",
            "gpt-4.1-nano",
        )
    }

    override fun translate(text: String, sourceLanguage: String, targetLanguage: String): String {
        for (model in fallbackModels) {
            try {
                return callApi(model, text)
            } catch (e: RestClientException) {
                log.warn { "Translation failed with $model: ${e.message}" }
            }
        }
        throw InternalServerException(ErrorCode.TRANSLATION_ALL_MODELS_FAILED)
    }

    private fun callApi(model: String, text: String): String {
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = SYSTEM_PROMPT),
                OpenAiMessage(role = "user", content = text),
            ),
        )

        val response = restClient.post()
            .uri(properties.apiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${properties.apiKey}")
            .body(request)
            .retrieve()
            .body(OpenAiChatResponse::class.java)
            ?: throw InternalServerException(ErrorCode.TRANSLATION_API_ERROR)

        return response.translatedText
    }
}
