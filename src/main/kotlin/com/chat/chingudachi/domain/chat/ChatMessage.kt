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

@Entity
@Table(name = "chat_message")
class ChatMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    val id: Long = 0,
    @Column(name = "chat_room_id", nullable = false)
    val chatRoomId: Long,
    @Column(name = "sender_id", nullable = false)
    val senderId: Long,
    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", nullable = false, length = 10)
    var messageStatus: MessageStatus = MessageStatus.SENT,
) : BaseTimeEntity() {
    companion object {
        const val MAX_CONTENT_LENGTH = 1000

        fun create(chatRoomId: Long, senderId: Long, content: String): ChatMessage {
            require(content.isNotBlank() && content.length <= MAX_CONTENT_LENGTH)
            return ChatMessage(chatRoomId = chatRoomId, senderId = senderId, content = content)
        }
    }
}

enum class MessageStatus {
    SENT,
    FAILED,
}
