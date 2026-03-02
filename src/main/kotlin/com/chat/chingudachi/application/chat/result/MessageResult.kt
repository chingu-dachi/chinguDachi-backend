package com.chat.chingudachi.application.chat.result

import java.time.Instant

data class SendMessageResult(
    val messageId: Long,
    val chatRoomId: Long,
    val senderId: Long,
    val content: String,
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
    val content: String,
    val messageStatus: String,
    val createdAt: Instant,
)
