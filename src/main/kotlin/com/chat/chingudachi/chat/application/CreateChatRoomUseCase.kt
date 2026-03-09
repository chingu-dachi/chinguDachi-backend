package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.application.result.ChatParticipant
import com.chat.chingudachi.chat.application.result.ChatRoomResult
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.chat.domain.RoomMember
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class CreateChatRoomUseCase(
    private val chatRoomStore: ChatRoomStore,
    private val roomMemberStore: RoomMemberStore,
    private val accountStore: AccountStore,
    private val txTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(accountId: Long, memberIds: List<Long>): ChatRoomResult {
        return try {
            executeInTx(accountId, memberIds)
        } catch (e: DataIntegrityViolationException) {
            log.info("Direct room race condition detected, retrying: accountId={}, memberIds={}", accountId, memberIds)
            executeInTx(accountId, memberIds)
        }
    }

    private fun executeInTx(accountId: Long, memberIds: List<Long>): ChatRoomResult {
        return txTemplate.execute { _ ->
            if (memberIds.size != 1) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_GROUP_NOT_SUPPORTED)
            }

            val targetId = memberIds.single()
            if (accountId == targetId) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_SELF_CHAT)
            }

            val target = accountStore.getById(targetId)
            if (!target.isActive()) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_TARGET_NOT_ACTIVE)
            }

            val existing = chatRoomStore.findDirectRoom(accountId, targetId)
            if (existing != null) {
                val members = roomMemberStore.findByRoomId(existing.id)
                return@execute assembleResult(existing, members)
            }

            val requester = accountStore.getById(accountId)
            // TODO: roomName은 생성 시점 닉네임 스냅샷 — 닉네임 변경 시 갱신 정책 필요 (Phase 2+)
            val roomName = "${requester.nickname?.value ?: "User"}, ${target.nickname?.value ?: "User"}"
            val chatRoom = chatRoomStore.save(
                ChatRoom.createDirect(
                    roomName = roomName,
                    roomOwnerId = accountId,
                    accountId1 = accountId,
                    accountId2 = targetId,
                ),
            )

            val members = roomMemberStore.saveAll(
                listOf(
                    RoomMember(chatRoom = chatRoom, accountId = accountId),
                    RoomMember(chatRoom = chatRoom, accountId = targetId),
                ),
            )

            log.info("Chat room created: roomId={}, pairKey={}", chatRoom.id, chatRoom.directPairKey)
            assembleResult(chatRoom, members)
        }!!
    }

    private fun assembleResult(chatRoom: ChatRoom, members: List<RoomMember>): ChatRoomResult {
        val accountIds = members.map { it.accountId }
        val accountsById = accountStore.findByIds(accountIds).associateBy { it.id }
        val participants = members.map { member ->
            val account = accountsById[member.accountId]
            ChatParticipant(
                userId = member.accountId,
                nickname = account?.nickname?.value,
                profileImageUrl = account?.profileImageUrl,
                nationality = account?.nation?.name,
                unreadCount = member.unreadCount,
            )
        }
        return ChatRoomResult(
            roomId = chatRoom.id,
            roomName = chatRoom.roomName,
            participants = participants,
            lastMessage = null,
            lastMessageAt = chatRoom.lastMessageAt,
            createdAt = chatRoom.createdAt,
        )
    }
}
