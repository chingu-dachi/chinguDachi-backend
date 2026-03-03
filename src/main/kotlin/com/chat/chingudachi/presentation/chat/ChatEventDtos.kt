package com.chat.chingudachi.presentation.chat

import java.time.Instant

// Client -> Server payloads
data class SendMessagePayload(val roomId: Long, val text: String)
data class TypingPayload(val roomId: Long)
data class ReadPayload(val roomId: Long)

// Server -> Client events (FE discriminated union 구조에 맞춤)
// NEW_MESSAGE → { type, message: Message }
// TYPING_START/STOP → { type, userId: string }
// READ → { type, data: { userId, readAt } }

data class MessageEventPayload(
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val originalText: String,
    val translatedText: String?,
    val status: String,
    val createdAt: Instant,
)

data class NewMessageEvent(
    val type: String = "NEW_MESSAGE",
    val message: MessageEventPayload,
)

data class TypingEvent(
    val type: String,
    val userId: String,
)

data class ReadEventPayload(val userId: String, val readAt: Instant)

data class ReadEvent(
    val type: String = "READ",
    val data: ReadEventPayload,
)

// ACK (sender confirmation)
data class MessageAck(val messageId: String, val status: String)

// Error event (sent to /user/queue/errors)
data class ChatErrorEvent(val code: String, val message: String)
