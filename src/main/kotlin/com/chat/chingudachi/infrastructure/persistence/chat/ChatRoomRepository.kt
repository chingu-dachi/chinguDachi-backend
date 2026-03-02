package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.domain.chat.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    @Query(
        """
        SELECT cr FROM ChatRoom cr
        WHERE cr.roomType = 'DIRECT'
        AND cr.roomStatus = 'ACTIVE'
        AND cr.directPairKey = :directPairKey
        """,
    )
    fun findDirectRoom(
        @Param("directPairKey") directPairKey: String,
    ): ChatRoom?
}
