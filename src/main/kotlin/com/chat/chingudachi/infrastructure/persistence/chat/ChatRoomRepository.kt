package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.domain.chat.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    @Query(
        """
        SELECT cr FROM ChatRoom cr
        WHERE cr.id IN (
            SELECT rm1.chatRoom.id FROM RoomMember rm1
            WHERE rm1.accountId = :accountId1 AND rm1.memberStatus = 'JOINED'
        )
        AND cr.id IN (
            SELECT rm2.chatRoom.id FROM RoomMember rm2
            WHERE rm2.accountId = :accountId2 AND rm2.memberStatus = 'JOINED'
        )
        AND cr.roomStatus = 'ACTIVE'
        """,
    )
    fun findByMembers(
        @Param("accountId1") accountId1: Long,
        @Param("accountId2") accountId2: Long,
    ): ChatRoom?
}
