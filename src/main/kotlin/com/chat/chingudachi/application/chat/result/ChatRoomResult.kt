package com.chat.chingudachi.application.chat.result

import java.time.Instant

data class ChatRoomResult(
    val roomId: Long,
    val roomName: String,
    val participants: List<ChatParticipant>,
    val lastMessageAt: Instant?,
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
    val nextCursor: Instant?,
    val hasMore: Boolean,
)
