package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.MessageItem
import com.chat.chingudachi.application.chat.result.MessagesResult
import com.chat.chingudachi.application.common.PaginationConstants
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMessagesUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
) {
    @Transactional(readOnly = true)
    fun execute(accountId: Long, roomId: Long, cursor: Long?): MessagesResult {
        chatRoomStore.getById(roomId)

        val member = roomMemberStore.findByRoomIdAndAccountId(roomId, accountId)
            ?: throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        if (!member.isJoined()) {
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }

        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val messages = chatMessageStore.findByRoomId(roomId, cursor, fetchSize)

        val hasMore = messages.size > PaginationConstants.DEFAULT_SIZE
        val pageMessages = if (hasMore) messages.dropLast(1) else messages

        val items = pageMessages.map { msg ->
            MessageItem(
                messageId = msg.id,
                senderId = msg.senderId,
                content = msg.content,
                messageStatus = msg.messageStatus.name,
                createdAt = msg.createdAt,
            )
        }

        val nextCursor = if (hasMore) pageMessages.lastOrNull()?.id else null

        return MessagesResult(items = items, nextCursor = nextCursor, hasMore = hasMore)
    }
}
