package com.chat.chingudachi.chat.controller

import java.time.Instant

data class SendMessagePayload(val roomId: Long, val text: String)
data class TypingPayload(val roomId: Long)
data class ReadPayload(val roomId: Long)

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

data class MessageAck(val messageId: String, val status: String)

data class ChatErrorEvent(val code: String, val message: String)
