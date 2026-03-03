package com.chat.chingudachi.application.chat.port

interface TranslationPort {
    fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}
