package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.getJoinedMember
import com.chat.chingudachi.application.chat.result.SendMessageResult
import com.chat.chingudachi.domain.chat.TranslationStatus
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RetryTranslationUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
    private val messageTranslationService: MessageTranslationService,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long, messageId: Long): SendMessageResult {
        chatRoomStore.getById(roomId)
        roomMemberStore.getJoinedMember(roomId, accountId)

        val message = chatMessageStore.getById(messageId)

        if (message.chatRoomId != roomId) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_ROOM_MISMATCH)
        }
        if (message.senderId == accountId) {
            throw BadRequestException(ErrorCode.CHAT_CANNOT_RETRY_OWN_MESSAGE)
        }
        if (message.translationStatus != TranslationStatus.FAILED) {
            throw BadRequestException(ErrorCode.CHAT_TRANSLATION_NOT_FAILED)
        }

        messageTranslationService.translateMessage(message)

        val saved = chatMessageStore.save(message)

        return SendMessageResult(
            messageId = saved.id,
            chatRoomId = roomId,
            senderId = saved.senderId,
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
