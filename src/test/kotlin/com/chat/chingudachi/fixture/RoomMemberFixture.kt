package com.chat.chingudachi.fixture

import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.chat.domain.MemberStatus
import com.chat.chingudachi.chat.domain.RoomMember
import java.time.Instant

object RoomMemberFixture {
    fun create(
        id: Long = 1L,
        chatRoom: ChatRoom = ChatRoomFixture.create(),
        accountId: Long = 1L,
        memberStatus: MemberStatus = MemberStatus.JOINED,
        unreadCount: Int = 0,
        lastReadAt: Instant? = null,
    ): RoomMember =
        RoomMember(
            id = id,
            chatRoom = chatRoom,
            accountId = accountId,
            memberStatus = memberStatus,
            unreadCount = unreadCount,
            lastReadAt = lastReadAt,
        )
}
