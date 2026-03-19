package com.chat.chingudachi.chat.infra

interface TranslationClient {
    fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}
