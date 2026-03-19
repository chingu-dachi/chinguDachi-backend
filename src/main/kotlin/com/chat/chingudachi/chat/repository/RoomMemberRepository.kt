package com.chat.chingudachi.chat.repository

import com.chat.chingudachi.chat.domain.RoomMember
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RoomMemberRepository : JpaRepository<RoomMember, Long> {
    @Modifying
    @Query(
        """
        UPDATE RoomMember rm
        SET rm.unreadCount = rm.unreadCount + 1
        WHERE rm.chatRoomId = :roomId
        AND rm.accountId != :excludeAccountId
        AND rm.memberStatus = 'JOINED'
        """,
    )
    fun incrementUnreadForOtherMembers(
        @Param("roomId") roomId: Long,
        @Param("excludeAccountId") excludeAccountId: Long,
    )

    @Query(
        """
        SELECT rm FROM RoomMember rm
        WHERE rm.accountId = :accountId
        AND rm.memberStatus = 'JOINED'
        AND rm.chatRoomId IN (
            SELECT cr.id FROM ChatRoom cr
            WHERE cr.roomStatus = 'ACTIVE'
            AND (:cursor IS NULL OR cr.id < :cursor)
        )
        ORDER BY rm.chatRoomId DESC
        """,
    )
    fun findJoinedByAccountId(
        @Param("accountId") accountId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<RoomMember>

    fun findByChatRoomIdIn(chatRoomIds: List<Long>): List<RoomMember>

    fun findByChatRoomId(chatRoomId: Long): List<RoomMember>

    fun findByChatRoomIdAndAccountId(chatRoomId: Long, accountId: Long): RoomMember?
}
