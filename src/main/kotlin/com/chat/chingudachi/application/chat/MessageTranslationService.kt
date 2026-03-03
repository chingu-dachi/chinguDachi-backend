package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.chat.ChatMessage
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
