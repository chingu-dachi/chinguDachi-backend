package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.domain.chat.RoomMember
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
        WHERE rm.chatRoom.id = :roomId
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
        JOIN FETCH rm.chatRoom cr
        WHERE rm.accountId = :accountId
        AND rm.memberStatus = 'JOINED'
        AND cr.roomStatus = 'ACTIVE'
        AND (:cursor IS NULL OR cr.id < :cursor)
        ORDER BY cr.id DESC
        """,
    )
    fun findByAccountId(
        @Param("accountId") accountId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<RoomMember>

    fun findByChatRoomIdIn(chatRoomIds: List<Long>): List<RoomMember>

    fun findByChatRoomId(chatRoomId: Long): List<RoomMember>

    fun findByChatRoomIdAndAccountId(chatRoomId: Long, accountId: Long): RoomMember?
}
