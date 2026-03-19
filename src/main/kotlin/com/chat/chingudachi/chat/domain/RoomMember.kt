package com.chat.chingudachi.chat.domain

import com.chat.chingudachi.core.domain.BaseTimeEntity
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
@Table(name = "room_member")
class RoomMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_member_id")
    val id: Long = 0,
    @Column(name = "chat_room_id", nullable = false)
    val chatRoomId: Long,
    @Column(name = "account_id", nullable = false)
    val accountId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 20)
    var memberStatus: MemberStatus = MemberStatus.JOINED,
    @Column(name = "unread_count", nullable = false)
    var unreadCount: Int = 0,
    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,
) : BaseTimeEntity() {
    fun incrementUnread() {
        unreadCount++
    }

    fun resetUnread() {
        unreadCount = 0
        lastReadAt = Instant.now()
    }

    fun isJoined(): Boolean = memberStatus == MemberStatus.JOINED
}

enum class MemberStatus {
    INVITED,
    JOINED,
    LEFT,
    KICKED,
    BANNED,
}
