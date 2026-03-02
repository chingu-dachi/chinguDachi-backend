package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.RoomMember

interface RoomMemberStore {
    fun save(roomMember: RoomMember): RoomMember
    fun saveAll(roomMembers: List<RoomMember>): List<RoomMember>
    fun findByAccountId(accountId: Long, cursor: Long?, size: Int): List<RoomMember>
    fun findByRoomId(roomId: Long): List<RoomMember>
    fun findByRoomIds(roomIds: List<Long>): List<RoomMember>
    fun findByRoomIdAndAccountId(roomId: Long, accountId: Long): RoomMember?
}
