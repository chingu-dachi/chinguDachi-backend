package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.domain.chat.RoomMember
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RoomMemberRepository : JpaRepository<RoomMember, Long> {
    @Query(
        """
        SELECT rm FROM RoomMember rm
        JOIN FETCH rm.chatRoom cr
        WHERE rm.accountId = :accountId
        AND rm.memberStatus = 'JOINED'
        AND cr.roomStatus = 'ACTIVE'
        ORDER BY cr.lastMessageAt DESC NULLS LAST
        """,
    )
    fun findByAccountId(
        @Param("accountId") accountId: Long,
        pageable: Pageable,
    ): List<RoomMember>

    @Query(
        """
        SELECT rm FROM RoomMember rm
        JOIN FETCH rm.chatRoom cr
        WHERE rm.accountId = :accountId
        AND rm.memberStatus = 'JOINED'
        AND cr.roomStatus = 'ACTIVE'
        AND cr.lastMessageAt < :cursor
        ORDER BY cr.lastMessageAt DESC NULLS LAST
        """,
    )
    fun findByAccountIdWithCursor(
        @Param("accountId") accountId: Long,
        @Param("cursor") cursor: Instant,
        pageable: Pageable,
    ): List<RoomMember>

    fun findByChatRoomId(chatRoomId: Long): List<RoomMember>

    fun findByChatRoomIdAndAccountId(chatRoomId: Long, accountId: Long): RoomMember?
}
