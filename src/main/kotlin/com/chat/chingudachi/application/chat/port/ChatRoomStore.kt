package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.ChatRoom
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException

interface ChatRoomStore {
    fun save(chatRoom: ChatRoom): ChatRoom
    fun findById(id: Long): ChatRoom?
    fun findDirectRoom(accountId1: Long, accountId2: Long): ChatRoom?

    fun getById(id: Long): ChatRoom =
        findById(id) ?: throw NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)
}
