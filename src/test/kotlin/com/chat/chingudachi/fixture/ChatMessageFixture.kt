package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.MessageStatus

object ChatMessageFixture {
    fun create(
        id: Long = 1L,
        chatRoomId: Long = 1L,
        senderId: Long = 1L,
        content: String = "Hello!",
        messageStatus: MessageStatus = MessageStatus.SENT,
    ): ChatMessage =
        ChatMessage(
            id = id,
            chatRoomId = chatRoomId,
            senderId = senderId,
            content = content,
            messageStatus = messageStatus,
        )
}
