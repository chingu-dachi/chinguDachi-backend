package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.ChatMessage

interface ChatMessageStore {
    fun save(message: ChatMessage): ChatMessage
    fun findByRoomId(roomId: Long, cursor: Long?, size: Int): List<ChatMessage>
}
