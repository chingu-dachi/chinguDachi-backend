package com.chat.chingudachi.domain.chat

import com.chat.chingudachi.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "chat_room")
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    val id: Long = 0,
    @Column(name = "room_name", nullable = false, length = 20)
    var roomName: String,
    @Column(name = "room_owner_id", nullable = false)
    val roomOwnerId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 10)
    var roomStatus: RoomStatus = RoomStatus.ACTIVE,
    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null,
) : BaseTimeEntity() {
    fun isActive(): Boolean = roomStatus == RoomStatus.ACTIVE

    fun updateLastMessageAt(instant: Instant) {
        lastMessageAt = instant
    }
}

enum class RoomStatus {
    ACTIVE,
    ARCHIVED,
}
