package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.TranslationPort
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.chat.domain.ChatMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class MessageTranslationService(
    private val translationPort: TranslationPort,
    private val accountStore: AccountStore,
) {
    fun translateMessage(message: ChatMessage) {
        val sender = accountStore.getById(message.senderId)
        val targetLanguage = sender.translateLanguage
        val nationName = sender.nation?.name?.lowercase() ?: return
        if (targetLanguage == null) return

        message.originalLanguage = nationName
        try {
            val translated = translationPort.translate(
                text = message.originalContent,
                sourceLanguage = nationName,
                targetLanguage = targetLanguage.name.lowercase(),
            )
            message.applyTranslation(translated, targetLanguage)
        } catch (e: Exception) {
            log.error(e) { "Translation failed for message ${message.id} in room ${message.chatRoomId}" }
            message.markTranslationFailed()
        }
    }
}
