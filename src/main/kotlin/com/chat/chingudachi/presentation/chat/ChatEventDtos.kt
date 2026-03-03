package com.chat.chingudachi.presentation.chat

import java.time.Instant

// Client -> Server payloads
data class SendMessagePayload(val roomId: Long, val text: String)
data class TypingPayload(val roomId: Long)
data class ReadPayload(val roomId: Long)

// Server -> Client events (FE ChatEvent 타입에 맞춤)
// MESSAGE → { type: "MESSAGE", data: MessageEventPayload }
// TYPING_START/STOP → { type, data: { userId } }
// READ → { type, data: { userId, readAt } }

data class MessageEventPayload(
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val originalContent: String,
    val translatedContent: String?,
    val status: String,
    val translationStatus: String,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
)

data class NewMessageEvent(
    val type: String = "MESSAGE",
    val data: MessageEventPayload,
)

data class TypingEventPayload(val userId: String)

data class TypingEvent(
    val type: String,
    val data: TypingEventPayload,
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
