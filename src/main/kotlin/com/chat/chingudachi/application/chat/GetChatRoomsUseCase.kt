package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.ChatParticipant
import com.chat.chingudachi.application.chat.result.ChatRoomResult
import com.chat.chingudachi.application.chat.result.ChatRoomsResult
import com.chat.chingudachi.application.common.PaginationConstants
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.chat.RoomMember
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetChatRoomsUseCase(
    private val roomMemberStore: RoomMemberStore,
    private val accountStore: AccountStore,
) {
    @Transactional(readOnly = true)
    fun execute(accountId: Long, cursor: Long?): ChatRoomsResult {
        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val members = roomMemberStore.findByAccountId(accountId, cursor, fetchSize)

        val hasMore = members.size > PaginationConstants.DEFAULT_SIZE
        val pageMembers = if (hasMore) members.dropLast(1) else members

        val roomIds = pageMembers.map { it.chatRoom.id }
        val membersByRoomId = loadRoomMembers(roomIds)
        val accountsById = loadAccounts(membersByRoomId.values.flatten())

        val items = buildChatRoomResults(pageMembers, membersByRoomId, accountsById)
        val nextCursor = if (hasMore) pageMembers.lastOrNull()?.chatRoom?.id else null

        return ChatRoomsResult(items, nextCursor, hasMore)
    }

    private fun loadRoomMembers(roomIds: List<Long>): Map<Long, List<RoomMember>> {
        if (roomIds.isEmpty()) return emptyMap()
        return roomMemberStore.findByRoomIds(roomIds).groupBy { it.chatRoom.id }
    }

    private fun loadAccounts(members: List<RoomMember>): Map<Long, Account> {
        val accountIds = members.map { it.accountId }.distinct()
        if (accountIds.isEmpty()) return emptyMap()
        return accountStore.findByIds(accountIds).associateBy { it.id }
    }

    private fun buildChatRoomResults(
        pageMembers: List<RoomMember>,
        membersByRoomId: Map<Long, List<RoomMember>>,
        accountsById: Map<Long, Account>,
    ): List<ChatRoomResult> = pageMembers.map { myMember ->
        val chatRoom = myMember.chatRoom
        val roomMembers = membersByRoomId[chatRoom.id] ?: emptyList()
        val participants = buildParticipants(roomMembers, accountsById)

        ChatRoomResult(
            roomId = chatRoom.id,
            roomName = chatRoom.roomName,
            participants = participants,
            lastMessageAt = chatRoom.lastMessageAt,
            createdAt = chatRoom.createdAt,
        )
    }

    private fun buildParticipants(
        members: List<RoomMember>,
        accountsById: Map<Long, Account>,
    ): List<ChatParticipant> = members.map { member ->
        val account = accountsById[member.accountId]
        ChatParticipant(
            userId = member.accountId,
            nickname = account?.nickname?.value,
            profileImageUrl = account?.profileImageUrl,
            nationality = account?.nation?.name,
            unreadCount = member.unreadCount,
        )
    }
}
