package com.chat.chingudachi.application.chat.port

import com.chat.chingudachi.domain.chat.RoomMember
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException

interface RoomMemberStore {
    fun save(roomMember: RoomMember): RoomMember
    fun saveAll(roomMembers: List<RoomMember>): List<RoomMember>
    fun findByAccountId(accountId: Long, cursor: Long?, size: Int): List<RoomMember>
    fun findByRoomId(roomId: Long): List<RoomMember>
    fun findByRoomIds(roomIds: List<Long>): List<RoomMember>
    fun findByRoomIdAndAccountId(roomId: Long, accountId: Long): RoomMember?
    fun incrementUnreadForOtherMembers(roomId: Long, excludeAccountId: Long)
}

fun RoomMemberStore.getJoinedMember(roomId: Long, accountId: Long): RoomMember {
    val member = findByRoomIdAndAccountId(roomId, accountId)
        ?: throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
    if (!member.isJoined()) {
        throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
    }
    return member
}
