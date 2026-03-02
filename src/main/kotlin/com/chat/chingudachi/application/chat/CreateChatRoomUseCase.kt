package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.ChatParticipant
import com.chat.chingudachi.application.chat.result.ChatRoomResult
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.chat.ChatRoom
import com.chat.chingudachi.domain.chat.RoomMember
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateChatRoomUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val accountStore: AccountStore,
) {
    @Transactional
    fun execute(accountId: Long, targetUserId: Long): ChatRoomResult {
        if (accountId == targetUserId) {
            throw BadRequestException(ErrorCode.CHAT_ROOM_SELF_CHAT)
        }

        val target = accountStore.getById(targetUserId)
        if (!target.isActive()) {
            throw BadRequestException(ErrorCode.CHAT_ROOM_TARGET_NOT_ACTIVE)
        }

        val existing = chatRoomStore.findByMembers(accountId, targetUserId)
        if (existing != null) {
            val members = roomMemberStore.findByRoomId(existing.id)
            return toResult(existing, members, accountId)
        }

        val me = accountStore.getById(accountId)
        val roomName = "${me.nickname?.value ?: "User"}, ${target.nickname?.value ?: "User"}"
        val chatRoom = chatRoomStore.save(ChatRoom(roomName = roomName, roomOwnerId = accountId))

        val members = roomMemberStore.saveAll(
            listOf(
                RoomMember(chatRoom = chatRoom, accountId = accountId),
                RoomMember(chatRoom = chatRoom, accountId = targetUserId),
            ),
        )

        return toResult(chatRoom, members, accountId)
    }

    private fun toResult(chatRoom: ChatRoom, members: List<RoomMember>, requesterId: Long): ChatRoomResult {
        val accounts = members.map { accountStore.getById(it.accountId) }
        val participants = members.map { member ->
            val account = accounts.first { it.id == member.accountId }
            ChatParticipant(
                userId = account.id,
                nickname = account.nickname?.value,
                profileImageUrl = account.profileImageUrl,
                nationality = account.nation?.name,
                unreadCount = member.unreadCount,
            )
        }
        return ChatRoomResult(
            roomId = chatRoom.id,
            roomName = chatRoom.roomName,
            participants = participants,
            lastMessageAt = chatRoom.lastMessageAt,
            createdAt = chatRoom.createdAt,
        )
    }
}
