package com.chat.chingudachi.chat.application.port

import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException

interface ChatMessageStore {
    fun save(message: ChatMessage): ChatMessage
    fun findById(id: Long): ChatMessage?
    fun findByRoomId(roomId: Long, cursor: Long?, size: Int): List<ChatMessage>

    fun findLatestByRoomIds(roomIds: List<Long>): List<ChatMessage>

    fun getById(id: Long): ChatMessage =
        findById(id) ?: throw NotFoundException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
}
