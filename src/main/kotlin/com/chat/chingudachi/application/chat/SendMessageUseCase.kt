package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.getJoinedMember
import com.chat.chingudachi.application.chat.result.SendMessageResult
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SendMessageUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
    private val messageTranslationService: MessageTranslationService,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long, content: String): SendMessageResult {
        val room = chatRoomStore.getById(roomId)
        if (!room.isActive()) {
            throw BadRequestException(ErrorCode.CHAT_ROOM_NOT_ACTIVE)
        }

        roomMemberStore.getJoinedMember(roomId, accountId)

        if (content.isBlank()) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_EMPTY)
        }
        if (content.length > ChatMessage.MAX_CONTENT_LENGTH) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_TOO_LONG)
        }

        val message = ChatMessage.create(chatRoomId = roomId, senderId = accountId, originalContent = content)

        messageTranslationService.translateMessage(message)

        val saved = chatMessageStore.save(message)

        val now = Instant.now()
        room.updateLastMessageAt(now)

        roomMemberStore.incrementUnreadForOtherMembers(roomId, accountId)

        return SendMessageResult(
            messageId = saved.id,
            chatRoomId = roomId,
            senderId = accountId,
            originalContent = saved.originalContent,
            messageStatus = saved.messageStatus,
            translatedContent = saved.translatedContent,
            translationStatus = saved.translationStatus,
            originalLanguage = saved.originalLanguage,
            translatedLanguage = saved.translatedLanguage,
            createdAt = saved.createdAt,
        )
    }
}
