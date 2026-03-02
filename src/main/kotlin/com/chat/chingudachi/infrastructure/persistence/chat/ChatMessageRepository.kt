package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.domain.chat.ChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    @Query(
        """
        SELECT m FROM ChatMessage m
        WHERE m.chatRoomId = :roomId
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
        """,
    )
    fun findByRoomId(
        @Param("roomId") roomId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<ChatMessage>
}
