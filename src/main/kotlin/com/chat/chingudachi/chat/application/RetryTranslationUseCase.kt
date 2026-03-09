package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatMessageStore
import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.application.port.getJoinedMember
import com.chat.chingudachi.chat.application.result.SendMessageResult
import com.chat.chingudachi.chat.domain.TranslationStatus
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
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
