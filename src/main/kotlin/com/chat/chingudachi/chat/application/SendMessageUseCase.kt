package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatMessageStore
import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.application.port.getJoinedMember
import com.chat.chingudachi.chat.application.result.SendMessageResult
import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
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
