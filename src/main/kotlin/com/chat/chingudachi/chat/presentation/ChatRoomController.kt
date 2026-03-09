package com.chat.chingudachi.chat.presentation

import com.chat.chingudachi.chat.application.CreateChatRoomUseCase
import com.chat.chingudachi.chat.application.GetChatRoomsUseCase
import com.chat.chingudachi.chat.application.GetMessagesUseCase
import com.chat.chingudachi.chat.application.RetryTranslationUseCase
import com.chat.chingudachi.chat.application.result.ChatParticipant
import com.chat.chingudachi.chat.application.result.ChatRoomResult
import com.chat.chingudachi.chat.application.result.LastMessageSummary
import com.chat.chingudachi.chat.application.result.MessageItem
import com.chat.chingudachi.chat.application.result.MessagesResult
import com.chat.chingudachi.core.presentation.AuthAccountId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import com.chat.chingudachi.core.presentation.PaginatedResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/chat-rooms")
class ChatRoomController(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val retryTranslationUseCase: RetryTranslationUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createChatRoom(
        @AuthAccountId accountId: Long,
        @Valid @RequestBody request: CreateChatRoomRequest,
    ): ChatRoomResponse {
        val result = createChatRoomUseCase.execute(accountId, request.memberIds)
        return ChatRoomResponse.from(result)
    }

    @GetMapping
    fun getChatRooms(
        @AuthAccountId accountId: Long,
        @RequestParam(required = false) cursor: Long?,
    ): PaginatedResponse<ChatRoomResponse> {
        val result = getChatRoomsUseCase.execute(accountId, cursor)
        return PaginatedResponse(
            items = result.items.map { ChatRoomResponse.from(it) },
            nextCursor = result.nextCursor,
            hasMore = result.hasMore,
        )
    }

    @GetMapping("/{roomId}/messages")
    fun getMessages(
        @AuthAccountId accountId: Long,
        @PathVariable roomId: Long,
        @RequestParam(required = false) cursor: Long?,
    ): PaginatedResponse<MessageResponse> {
        val result = getMessagesUseCase.execute(accountId, roomId, cursor)
        return PaginatedResponse(
            items = result.items.map { MessageResponse.from(it, roomId) },
            nextCursor = result.nextCursor,
            hasMore = result.hasMore,
        )
    }

    @PostMapping("/{roomId}/messages/{messageId}/retry-translation")
    fun retryTranslation(
        @AuthAccountId accountId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
    ): MessageResponse {
        val result = retryTranslationUseCase.execute(accountId, roomId, messageId)
        return MessageResponse(
            messageId = result.messageId.toString(),
            roomId = result.chatRoomId.toString(),
            senderId = result.senderId.toString(),
            originalContent = result.originalContent,
            translatedContent = result.translatedContent,
            status = result.messageStatus.name,
            translationStatus = result.translationStatus.name,
            originalLanguage = result.originalLanguage,
            translatedLanguage = result.translatedLanguage,
            createdAt = result.createdAt,
        )
    }
}

data class CreateChatRoomRequest(
    @field:NotEmpty
    val memberIds: List<Long>,
)

data class ChatRoomResponse(
    val roomId: String,
    val roomName: String,
    val participants: List<ParticipantResponse>,
    val lastMessage: LastMessageResponse?,
    val lastMessageAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(result: ChatRoomResult) = ChatRoomResponse(
            roomId = result.roomId.toString(),
            roomName = result.roomName,
            participants = result.participants.map { ParticipantResponse.from(it) },
            lastMessage = result.lastMessage?.let { LastMessageResponse.from(it) },
            lastMessageAt = result.lastMessageAt,
            createdAt = result.createdAt,
        )
    }
}

data class LastMessageResponse(
    val messageId: String,
    val senderId: String,
    val originalContent: String,
    val translatedContent: String?,
    val translationStatus: String,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(summary: LastMessageSummary) = LastMessageResponse(
            messageId = summary.messageId.toString(),
            senderId = summary.senderId.toString(),
            originalContent = summary.originalContent,
            translatedContent = summary.translatedContent,
            translationStatus = summary.translationStatus.name,
            originalLanguage = summary.originalLanguage,
            translatedLanguage = summary.translatedLanguage,
            createdAt = summary.createdAt,
        )
    }
}

data class ParticipantResponse(
    val userId: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val nationality: String?,
    val unreadCount: Int,
) {
    companion object {
        fun from(participant: ChatParticipant) = ParticipantResponse(
            userId = participant.userId.toString(),
            nickname = participant.nickname,
            profileImageUrl = participant.profileImageUrl,
            nationality = participant.nationality,
            unreadCount = participant.unreadCount,
        )
    }
}

data class MessageResponse(
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val originalContent: String,
    val translatedContent: String?,
    val status: String,
    val translationStatus: String,
    val originalLanguage: String?,
    val translatedLanguage: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(item: MessageItem, roomId: Long) = MessageResponse(
            messageId = item.messageId.toString(),
            roomId = roomId.toString(),
            senderId = item.senderId.toString(),
            originalContent = item.originalContent,
            translatedContent = item.translatedContent,
            status = item.messageStatus.name,
            translationStatus = item.translationStatus.name,
            originalLanguage = item.originalLanguage,
            translatedLanguage = item.translatedLanguage,
            createdAt = item.createdAt,
        )
    }
}
