package com.chat.chingudachi.chat.application.result

import com.chat.chingudachi.chat.domain.MessageStatus
import com.chat.chingudachi.chat.domain.TranslationStatus
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
