package com.chat.chingudachi.fixture

import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.chat.domain.MessageStatus
import com.chat.chingudachi.chat.domain.TranslationStatus

object ChatMessageFixture {
    fun create(
        id: Long = 1L,
        chatRoomId: Long = 1L,
        senderId: Long = 1L,
        originalContent: String = "Hello!",
        messageStatus: MessageStatus = MessageStatus.SENT,
        translatedContent: String? = null,
        translationStatus: TranslationStatus = TranslationStatus.NONE,
        originalLanguage: String? = null,
        translatedLanguage: String? = null,
    ): ChatMessage =
        ChatMessage(
            id = id,
            chatRoomId = chatRoomId,
            senderId = senderId,
            originalContent = originalContent,
            messageStatus = messageStatus,
            translatedContent = translatedContent,
            translationStatus = translationStatus,
            originalLanguage = originalLanguage,
            translatedLanguage = translatedLanguage,
        )
}
