package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.application.chat.port.getJoinedMember
import com.chat.chingudachi.application.chat.result.MessageItem
import com.chat.chingudachi.application.chat.result.MessagesResult
import com.chat.chingudachi.application.common.PaginationConstants
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.TranslationStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class GetMessagesUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val chatMessageStore: ChatMessageStore,
    private val translationPort: TranslationPort,
    private val accountStore: AccountStore,
) {
    @Transactional
    fun execute(accountId: Long, roomId: Long, cursor: Long?): MessagesResult {
        chatRoomStore.getById(roomId)

        roomMemberStore.getJoinedMember(roomId, accountId)

        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val messages = chatMessageStore.findByRoomId(roomId, cursor, fetchSize)

        val hasMore = messages.size > PaginationConstants.DEFAULT_SIZE
        val pageMessages = if (hasMore) messages.dropLast(1) else messages

        retryFailedTranslations(pageMessages)

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

    private fun retryFailedTranslations(messages: List<ChatMessage>) {
        val failedMessages = messages.filter { it.translationStatus == TranslationStatus.FAILED }
        if (failedMessages.isEmpty()) return

        val senderIds = failedMessages.map { it.senderId }.distinct()
        val senders = accountStore.findByIds(senderIds).associateBy { it.id }

        for (msg in failedMessages) {
            val sender = senders[msg.senderId] ?: continue
            val targetLang = sender.translateLanguage ?: continue
            val sourceLang = msg.originalLanguage ?: sender.nation?.name?.lowercase() ?: continue

            try {
                val translated = translationPort.translate(
                    text = msg.originalContent,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang.name.lowercase(),
                )
                msg.applyTranslation(translated, targetLang)
                log.info { "Translation retry succeeded: messageId=${msg.id}" }
            } catch (e: Exception) {
                log.warn(e) { "Translation retry failed: messageId=${msg.id}" }
            }
        }
    }
}
