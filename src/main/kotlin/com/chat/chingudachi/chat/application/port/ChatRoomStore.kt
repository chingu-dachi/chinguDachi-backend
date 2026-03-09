package com.chat.chingudachi.chat.application.port

import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException

interface ChatRoomStore {
    fun save(chatRoom: ChatRoom): ChatRoom
    fun findById(id: Long): ChatRoom?
    fun findDirectRoom(accountId1: Long, accountId2: Long): ChatRoom?

    fun getById(id: Long): ChatRoom =
        findById(id) ?: throw NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)
}
