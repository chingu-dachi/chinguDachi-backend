package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatMessageStore
import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.application.port.getJoinedMember
import com.chat.chingudachi.chat.application.result.MessageItem
import com.chat.chingudachi.chat.application.result.MessagesResult
import com.chat.chingudachi.core.application.PaginationConstants
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

        roomMemberStore.getJoinedMember(roomId, accountId)

        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val messages = chatMessageStore.findByRoomId(roomId, cursor, fetchSize)

        val hasMore = messages.size > PaginationConstants.DEFAULT_SIZE
        val pageMessages = if (hasMore) messages.dropLast(1) else messages

        val items = pageMessages.map { msg ->
            MessageItem(
                messageId = msg.id,
                senderId = msg.senderId,
                originalContent = msg.originalContent,
                messageStatus = msg.messageStatus,
                translatedContent = msg.translatedContent,
                translationStatus = msg.translationStatus,
                originalLanguage = msg.originalLanguage,
                translatedLanguage = msg.translatedLanguage,
                createdAt = msg.createdAt,
            )
        }

        val nextCursor = if (hasMore) pageMessages.lastOrNull()?.id else null

        return MessagesResult(items = items, nextCursor = nextCursor, hasMore = hasMore)
    }
}
