package com.chat.chingudachi.application.chat.result

import com.chat.chingudachi.domain.chat.MessageStatus
import com.chat.chingudachi.domain.chat.TranslationStatus
import java.time.Instant

data class SendMessageResult(
    val messageId: Long,
    val chatRoomId: Long,
    val senderId: Long,
    val originalContent: String,
    val messageStatus: MessageStatus,
    val translatedContent: String?,
    val translationStatus: TranslationStatus,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
)

data class MessagesResult(
    val items: List<MessageItem>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)

data class MessageItem(
    val messageId: Long,
    val senderId: Long,
    val originalContent: String,
    val messageStatus: MessageStatus,
    val translatedContent: String?,
    val translationStatus: TranslationStatus,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
)
