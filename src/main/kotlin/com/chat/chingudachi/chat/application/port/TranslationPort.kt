package com.chat.chingudachi.chat.application.port

interface TranslationPort {
    fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}
