package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.ChatParticipant
import com.chat.chingudachi.application.chat.result.ChatRoomResult
import com.chat.chingudachi.application.chat.result.ChatRoomsResult
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.chat.RoomMember
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GetChatRoomsUseCase(
    private val roomMemberStore: RoomMemberStore,
    private val accountStore: AccountStore,
) {
    @Transactional(readOnly = true)
    fun execute(accountId: Long, cursor: Instant?, size: Int): ChatRoomsResult {
        val members = roomMemberStore.findByAccountId(accountId, cursor, size + 1)

        val hasMore = members.size > size
        val resultMembers = if (hasMore) members.dropLast(1) else members

        val items = resultMembers.map { member -> toResult(member, accountId) }
        val nextCursor = if (hasMore) resultMembers.lastOrNull()?.chatRoom?.lastMessageAt else null

        return ChatRoomsResult(items, nextCursor, hasMore)
    }

    private fun toResult(myMember: RoomMember, accountId: Long): ChatRoomResult {
        val chatRoom = myMember.chatRoom
        val allMembers = roomMemberStore.findByRoomId(chatRoom.id)

        val participants = allMembers.map { member ->
            val account = accountStore.getById(member.accountId)
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
