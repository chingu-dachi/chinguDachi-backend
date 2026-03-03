package com.chat.chingudachi.presentation.chat

import java.time.Instant

// Client -> Server payloads
data class SendMessagePayload(val roomId: Long, val text: String)
data class TypingPayload(val roomId: Long)
data class ReadPayload(val roomId: Long)

// Server -> Client events
sealed interface ChatEventData

data class ChatEvent(val type: ChatEventType, val data: ChatEventData)

enum class ChatEventType {
    NEW_MESSAGE,
    TYPING_START,
    TYPING_STOP,
    READ,
}

data class MessageEventData(
    val messageId: Long,
    val roomId: Long,
    val senderId: Long,
    val originalText: String,
    val translatedText: String?,
    val status: String,
    val createdAt: Instant,
) : ChatEventData

data class TypingEventData(val userId: Long) : ChatEventData
data class ReadEventData(val userId: Long, val readAt: Instant) : ChatEventData

// ACK (sender confirmation)
data class MessageAck(val messageId: Long, val status: String)

// Error event (sent to /user/queue/errors)
data class ChatErrorEvent(val code: String, val message: String)
