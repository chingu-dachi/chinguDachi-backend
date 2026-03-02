package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.SendMessageResult
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SendMessageUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long, content: String): SendMessageResult {
        val room = chatRoomStore.getById(roomId)
        if (!room.isActive()) {
            throw BadRequestException(ErrorCode.CHAT_ROOM_NOT_ACTIVE)
        }

        val member = roomMemberStore.findByRoomIdAndAccountId(roomId, accountId)
            ?: throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        if (!member.isJoined()) {
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }

        if (content.isBlank()) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_EMPTY)
        }
        if (content.length > ChatMessage.MAX_CONTENT_LENGTH) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_TOO_LONG)
        }

        val message = chatMessageStore.save(
            ChatMessage.create(chatRoomId = roomId, senderId = accountId, content = content),
        )

        val now = Instant.now()
        room.updateLastMessageAt(now)

        val otherMembers = roomMemberStore.findByRoomId(roomId)
            .filter { it.accountId != accountId && it.isJoined() }
        otherMembers.forEach { it.incrementUnread() }

        return SendMessageResult(
            messageId = message.id,
            chatRoomId = roomId,
            senderId = accountId,
            content = message.content,
            createdAt = message.createdAt,
        )
    }
}
