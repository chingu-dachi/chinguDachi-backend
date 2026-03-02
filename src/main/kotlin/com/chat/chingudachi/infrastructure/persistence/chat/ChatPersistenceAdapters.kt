package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.chat.ChatRoom
import com.chat.chingudachi.domain.chat.RoomMember
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ChatRoomStoreAdapter(
    private val chatRoomRepository: ChatRoomRepository,
) : ChatRoomStore {
    override fun save(chatRoom: ChatRoom): ChatRoom = chatRoomRepository.save(chatRoom)

    override fun findById(id: Long): ChatRoom? = chatRoomRepository.findByIdOrNull(id)

    override fun findByMembers(accountId1: Long, accountId2: Long): ChatRoom? =
        chatRoomRepository.findByMembers(accountId1, accountId2)
}

@Repository
class RoomMemberStoreAdapter(
    private val roomMemberRepository: RoomMemberRepository,
) : RoomMemberStore {
    override fun save(roomMember: RoomMember): RoomMember = roomMemberRepository.save(roomMember)

    override fun saveAll(roomMembers: List<RoomMember>): List<RoomMember> =
        roomMemberRepository.saveAll(roomMembers)

    override fun findByAccountId(accountId: Long, cursor: Instant?, size: Int): List<RoomMember> {
        val pageable = PageRequest.of(0, size)
        return if (cursor != null) {
            roomMemberRepository.findByAccountIdWithCursor(accountId, cursor, pageable)
        } else {
            roomMemberRepository.findByAccountId(accountId, pageable)
        }
    }

    override fun findByRoomId(roomId: Long): List<RoomMember> =
        roomMemberRepository.findByChatRoomId(roomId)

    override fun findByRoomIdAndAccountId(roomId: Long, accountId: Long): RoomMember? =
        roomMemberRepository.findByChatRoomIdAndAccountId(roomId, accountId)
}
