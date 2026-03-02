package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.chat.ChatRoom
import com.chat.chingudachi.domain.chat.RoomStatus
import java.time.Instant

object ChatRoomFixture {
    fun create(
        id: Long = 1L,
        roomName: String = "테스트, ユーザー",
        roomOwnerId: Long = 1L,
        roomStatus: RoomStatus = RoomStatus.ACTIVE,
        lastMessageAt: Instant? = null,
    ): ChatRoom =
        ChatRoom(
            id = id,
            roomName = roomName,
            roomOwnerId = roomOwnerId,
            roomStatus = roomStatus,
            lastMessageAt = lastMessageAt,
        )
}
