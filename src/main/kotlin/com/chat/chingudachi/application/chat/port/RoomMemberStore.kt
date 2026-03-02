package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.RoomMember
import java.time.Instant

interface RoomMemberStore {
    fun save(roomMember: RoomMember): RoomMember
    fun saveAll(roomMembers: List<RoomMember>): List<RoomMember>
    fun findByAccountId(accountId: Long, cursor: Instant?, size: Int): List<RoomMember>
    fun findByRoomId(roomId: Long): List<RoomMember>
    fun findByRoomIdAndAccountId(roomId: Long, accountId: Long): RoomMember?
}
