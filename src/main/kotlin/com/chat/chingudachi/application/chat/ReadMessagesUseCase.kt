package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReadMessagesUseCase(
    private val roomMemberStore: RoomMemberStore,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long) {
        val member = roomMemberStore.findByRoomIdAndAccountId(roomId, accountId)
            ?: throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        if (!member.isJoined()) {
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }

        member.resetUnread()
    }
}
