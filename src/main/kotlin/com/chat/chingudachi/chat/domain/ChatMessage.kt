package com.chat.chingudachi.chat.domain

import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "chat_message")
class ChatMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    val id: Long = 0,
    @Column(name = "chat_room_id", nullable = false)
    val chatRoomId: Long,
    @Column(name = "sender_id", nullable = false)
    val senderId: Long,
    @Column(name = "original_content", nullable = false, length = MAX_CONTENT_LENGTH)
    val originalContent: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", nullable = false, length = 10)
    var messageStatus: MessageStatus = MessageStatus.SENT,
    @Column(name = "translated_content", length = MAX_CONTENT_LENGTH)
    var translatedContent: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false, length = 10)
    var translationStatus: TranslationStatus = TranslationStatus.NONE,
    @Column(name = "original_language", length = 2)
    var originalLanguage: String? = null,
    @Column(name = "translated_language", length = 2)
    var translatedLanguage: String? = null,
) : BaseTimeEntity() {

    fun applyTranslation(translated: String, targetLang: TranslateLanguage) {
        this.translatedContent = translated
        this.translationStatus = TranslationStatus.DONE
        this.translatedLanguage = targetLang.name.lowercase()
    }

    fun markTranslationFailed() {
        this.translationStatus = TranslationStatus.FAILED
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 1000

        fun create(chatRoomId: Long, senderId: Long, originalContent: String): ChatMessage {
            require(originalContent.isNotBlank() && originalContent.length <= MAX_CONTENT_LENGTH) {
                "Domain invariant violation: content must be non-blank and <= $MAX_CONTENT_LENGTH chars"
            }
            return ChatMessage(chatRoomId = chatRoomId, senderId = senderId, originalContent = originalContent)
        }
    }
}

enum class MessageStatus {
    SENT,
    FAILED,
}

enum class TranslationStatus {
    NONE,
    DONE,
    FAILED,
}
