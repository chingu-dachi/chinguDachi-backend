package com.chat.chingudachi.infrastructure.persistence.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.ChatRoom
import com.chat.chingudachi.domain.chat.RoomMember
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ChatRoomStoreAdapter(
    private val chatRoomRepository: ChatRoomRepository,
) : ChatRoomStore {
    override fun save(chatRoom: ChatRoom): ChatRoom = chatRoomRepository.save(chatRoom)

    override fun findById(id: Long): ChatRoom? = chatRoomRepository.findByIdOrNull(id)

    override fun findDirectRoom(accountId1: Long, accountId2: Long): ChatRoom? =
        chatRoomRepository.findDirectRoom(
            directPairKey = ChatRoom.buildPairKey(accountId1, accountId2),
        )
}

@Repository
class RoomMemberStoreAdapter(
    private val roomMemberRepository: RoomMemberRepository,
) : RoomMemberStore {
    override fun save(roomMember: RoomMember): RoomMember = roomMemberRepository.save(roomMember)

    override fun saveAll(roomMembers: List<RoomMember>): List<RoomMember> =
        roomMemberRepository.saveAll(roomMembers)

    override fun findByAccountId(accountId: Long, cursor: Long?, size: Int): List<RoomMember> =
        roomMemberRepository.findByAccountId(accountId, cursor, PageRequest.of(0, size))

    override fun findByRoomId(roomId: Long): List<RoomMember> =
        roomMemberRepository.findByChatRoomId(roomId)

    override fun findByRoomIds(roomIds: List<Long>): List<RoomMember> {
        if (roomIds.isEmpty()) return emptyList()
        return roomMemberRepository.findByChatRoomIdIn(roomIds)
    }

    override fun findByRoomIdAndAccountId(roomId: Long, accountId: Long): RoomMember? =
        roomMemberRepository.findByChatRoomIdAndAccountId(roomId, accountId)

    override fun incrementUnreadForOtherMembers(roomId: Long, excludeAccountId: Long) {
        roomMemberRepository.incrementUnreadForOtherMembers(roomId, excludeAccountId)
    }
}

@Repository
class ChatMessageStoreAdapter(
    private val chatMessageRepository: ChatMessageRepository,
) : ChatMessageStore {
    override fun save(message: ChatMessage): ChatMessage = chatMessageRepository.save(message)

    override fun findById(id: Long): ChatMessage? = chatMessageRepository.findByIdOrNull(id)

    override fun findByRoomId(roomId: Long, cursor: Long?, size: Int): List<ChatMessage> =
        chatMessageRepository.findByRoomId(roomId, cursor, PageRequest.of(0, size))
}
