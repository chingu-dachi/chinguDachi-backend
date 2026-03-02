package com.chat.chingudachi.presentation.chat

import java.time.Instant

// Client -> Server payloads
data class SendMessagePayload(val roomId: Long, val text: String)
data class TypingPayload(val roomId: Long)
data class ReadPayload(val roomId: Long)

// Server -> Client events
data class ChatEvent(val type: ChatEventType, val data: Any)

enum class ChatEventType {
    MESSAGE,
    TYPING_START,
    TYPING_STOP,
    READ,
}

data class MessageEventData(
    val messageId: Long,
    val roomId: Long,
    val senderId: Long,
    val content: String,
    val status: String,
    val createdAt: Instant,
)

data class TypingEventData(val userId: Long)
data class ReadEventData(val userId: Long, val readAt: Instant)

// ACK (sender confirmation)
data class MessageAck(val messageId: Long, val status: String)
