package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.getJoinedMember
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReadMessagesUseCase(
    private val roomMemberStore: RoomMemberStore,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long) {
        val member = roomMemberStore.getJoinedMember(roomId, accountId)
        member.resetUnread()
    }
}
