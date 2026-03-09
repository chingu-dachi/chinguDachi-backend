package com.chat.chingudachi.fixture

import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.chat.domain.RoomStatus
import com.chat.chingudachi.chat.domain.RoomType
import java.time.Instant

object ChatRoomFixture {
    fun create(
        id: Long = 1L,
        roomName: String = "테스트, ユーザー",
        roomOwnerId: Long = 1L,
        roomType: RoomType = RoomType.DIRECT,
        roomStatus: RoomStatus = RoomStatus.ACTIVE,
        directPairKey: String? = null,
        lastMessageAt: Instant? = null,
    ): ChatRoom =
        ChatRoom(
            id = id,
            roomName = roomName,
            roomOwnerId = roomOwnerId,
            roomType = roomType,
            roomStatus = roomStatus,
            directPairKey = directPairKey,
            lastMessageAt = lastMessageAt,
        )
}
