package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException

interface ChatMessageStore {
    fun save(message: ChatMessage): ChatMessage
    fun findById(id: Long): ChatMessage?
    fun findByRoomId(roomId: Long, cursor: Long?, size: Int): List<ChatMessage>

    fun getById(id: Long): ChatMessage =
        findById(id) ?: throw NotFoundException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
}
