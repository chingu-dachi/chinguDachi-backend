package com.chat.chingudachi.fixture

import com.chat.chingudachi.chat.domain.MemberStatus
import com.chat.chingudachi.chat.domain.RoomMember
import java.time.Instant

object RoomMemberFixture {
    fun create(
        id: Long = 1L,
        chatRoomId: Long = 1L,
        accountId: Long = 1L,
        memberStatus: MemberStatus = MemberStatus.JOINED,
        unreadCount: Int = 0,
        lastReadAt: Instant? = null,
    ): RoomMember =
        RoomMember(
            id = id,
            chatRoomId = chatRoomId,
            accountId = accountId,
            memberStatus = memberStatus,
            unreadCount = unreadCount,
            lastReadAt = lastReadAt,
        )
}
