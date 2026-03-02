package com.chat.chingudachi.presentation.chat

import com.chat.chingudachi.application.chat.CreateChatRoomUseCase
import com.chat.chingudachi.application.chat.GetChatRoomsUseCase
import com.chat.chingudachi.application.chat.result.ChatParticipant
import com.chat.chingudachi.application.chat.result.ChatRoomResult
import com.chat.chingudachi.presentation.common.AuthAccountId
import com.chat.chingudachi.presentation.common.PaginatedResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
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
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createChatRoom(
        @AuthAccountId accountId: Long,
        @RequestBody request: CreateChatRoomRequest,
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
}

data class CreateChatRoomRequest(
    val memberIds: List<Long>,
)

data class ChatRoomResponse(
    val roomId: String,
    val roomName: String,
    val participants: List<ParticipantResponse>,
    val lastMessageAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(result: ChatRoomResult) = ChatRoomResponse(
            roomId = result.roomId.toString(),
            roomName = result.roomName,
            participants = result.participants.map { ParticipantResponse.from(it) },
            lastMessageAt = result.lastMessageAt,
            createdAt = result.createdAt,
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
