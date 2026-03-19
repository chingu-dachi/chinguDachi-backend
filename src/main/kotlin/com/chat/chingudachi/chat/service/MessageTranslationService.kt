package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.chat.infra.TranslationClient
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class MessageTranslationService(
    private val translationClient: TranslationClient,
    private val accountRepository: AccountRepository,
) {
    fun translateMessage(message: ChatMessage) {
        val sender = accountRepository.findByIdOrNull(message.senderId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        val targetLanguage = sender.translateLanguage
        val nationName = sender.nation?.name?.lowercase() ?: return
        if (targetLanguage == null) return

        message.originalLanguage = nationName
        try {
            val translated = translationClient.translate(
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
