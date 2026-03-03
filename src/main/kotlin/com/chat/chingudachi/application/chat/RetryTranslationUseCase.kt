package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.application.chat.port.getJoinedMember
import com.chat.chingudachi.application.chat.result.SendMessageResult
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.chat.TranslationStatus
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class RetryTranslationUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
    private val translationPort: TranslationPort,
    private val accountStore: AccountStore,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long, messageId: Long): SendMessageResult {
        chatRoomStore.getById(roomId)
        roomMemberStore.getJoinedMember(roomId, accountId)

        val message = chatMessageStore.getById(messageId)

        if (message.chatRoomId != roomId) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_ROOM_MISMATCH)
        }
        if (message.translationStatus != TranslationStatus.FAILED) {
            throw BadRequestException(ErrorCode.CHAT_TRANSLATION_NOT_FAILED)
        }

        val sender = accountStore.getById(message.senderId)
        val targetLanguage = sender.translateLanguage
        val nationName = sender.nation?.name?.lowercase()

        if (targetLanguage != null && nationName != null) {
            try {
                val translated = translationPort.translate(
                    text = message.originalContent,
                    sourceLanguage = nationName,
                    targetLanguage = targetLanguage.name.lowercase(),
                )
                message.applyTranslation(translated, targetLanguage)
            } catch (e: Exception) {
                log.error(e) { "Translation retry failed for message $messageId in room $roomId" }
                message.markTranslationFailed()
            }
        }

        val saved = chatMessageStore.save(message)

        return SendMessageResult(
            messageId = saved.id,
            chatRoomId = roomId,
            senderId = saved.senderId,
            originalContent = saved.originalContent,
            translatedContent = saved.translatedContent,
            translationStatus = saved.translationStatus,
            originalLanguage = saved.originalLanguage,
            translatedLanguage = saved.translatedLanguage,
            createdAt = saved.createdAt,
        )
    }
}
