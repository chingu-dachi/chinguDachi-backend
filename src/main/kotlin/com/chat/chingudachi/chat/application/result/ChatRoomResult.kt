package com.chat.chingudachi.chat.application.result

import com.chat.chingudachi.chat.domain.TranslationStatus
import java.time.Instant

data class ChatRoomResult(
    val roomId: Long,
    val roomName: String,
    val participants: List<ChatParticipant>,
    val lastMessage: LastMessageSummary?,
    val lastMessageAt: Instant?,
    val createdAt: Instant,
)

data class LastMessageSummary(
    val messageId: Long,
    val senderId: Long,
    val originalContent: String,
    val translatedContent: String?,
    val translationStatus: TranslationStatus,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
)

data class ChatParticipant(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
    val nationality: String?,
    val unreadCount: Int,
)

data class ChatRoomsResult(
    val items: List<ChatRoomResult>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
