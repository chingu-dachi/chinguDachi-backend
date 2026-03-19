package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.chat.domain.RoomMember
import com.chat.chingudachi.chat.domain.TranslationStatus
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.core.application.PaginationConstants
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
import com.chat.chingudachi.core.domain.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val accountRepository: AccountRepository,
    private val messageTranslationService: MessageTranslationService,
    private val txTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sendMessage(accountId: Long, roomId: Long, content: String): SendMessageResult {
        val room = findChatRoomOrThrow(roomId)
        if (!room.isActive()) {
            throw BadRequestException(ErrorCode.CHAT_ROOM_NOT_ACTIVE)
        }

        getJoinedMember(roomId, accountId)

        if (content.isBlank()) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_EMPTY)
        }
        if (content.length > ChatMessage.MAX_CONTENT_LENGTH) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_TOO_LONG)
        }

        val message = ChatMessage.create(chatRoomId = roomId, senderId = accountId, originalContent = content)

        messageTranslationService.translateMessage(message)

        val saved = chatMessageRepository.save(message)

        room.updateLastMessageAt(Instant.now())

        roomMemberRepository.incrementUnreadForOtherMembers(roomId, accountId)

        return toSendMessageResult(saved, roomId)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(accountId: Long, cursor: Long?): ChatRoomsResult {
        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val members = roomMemberRepository.findJoinedByAccountId(accountId, cursor, PageRequest.of(0, fetchSize))

        val hasMore = members.size > PaginationConstants.DEFAULT_SIZE
        val pageMembers = if (hasMore) members.dropLast(1) else members

        val roomIds = pageMembers.map { it.chatRoomId }
        if (roomIds.isEmpty()) return ChatRoomsResult(emptyList(), null, false)
        val roomsById = chatRoomRepository.findAllById(roomIds).associateBy { it.id }
        val membersByRoomId = loadRoomMembers(roomIds)
        val accountsById = loadAccounts(membersByRoomId.values.flatten())
        val lastMessagesByRoomId = loadLastMessages(roomIds)

        val items = pageMembers.mapNotNull { myMember ->
            val chatRoom = roomsById[myMember.chatRoomId] ?: return@mapNotNull null
            val roomMembers = membersByRoomId[chatRoom.id] ?: emptyList()
            val participants = buildParticipants(roomMembers, accountsById)

            ChatRoomResult(
                roomId = chatRoom.id,
                roomName = chatRoom.roomName,
                participants = participants,
                lastMessage = lastMessagesByRoomId[chatRoom.id]?.toSummary(),
                lastMessageAt = chatRoom.lastMessageAt,
                createdAt = chatRoom.createdAt,
            )
        }
        val nextCursor = if (hasMore) pageMembers.lastOrNull()?.chatRoomId else null

        return ChatRoomsResult(items, nextCursor, hasMore)
    }

    @Transactional(readOnly = true)
    fun getMessages(accountId: Long, roomId: Long, cursor: Long?): MessagesResult {
        findChatRoomOrThrow(roomId)
        getJoinedMember(roomId, accountId)

        val fetchSize = PaginationConstants.DEFAULT_SIZE + 1
        val messages = chatMessageRepository.findByRoomId(roomId, cursor, PageRequest.of(0, fetchSize))

        val hasMore = messages.size > PaginationConstants.DEFAULT_SIZE
        val pageMessages = if (hasMore) messages.dropLast(1) else messages

        val items = pageMessages.map { msg ->
            MessageItem(
                messageId = msg.id,
                senderId = msg.senderId,
                originalContent = msg.originalContent,
                messageStatus = msg.messageStatus,
                translatedContent = msg.translatedContent,
                translationStatus = msg.translationStatus,
                originalLanguage = msg.originalLanguage,
                translatedLanguage = msg.translatedLanguage,
                createdAt = msg.createdAt,
            )
        }

        val nextCursor = if (hasMore) pageMessages.lastOrNull()?.id else null
        return MessagesResult(items = items, nextCursor = nextCursor, hasMore = hasMore)
    }

    fun createChatRoom(accountId: Long, memberIds: List<Long>): ChatRoomResult {
        return try {
            createChatRoomInTx(accountId, memberIds)
        } catch (e: DataIntegrityViolationException) {
            log.info("Direct room race condition detected, retrying: accountId={}, memberIds={}", accountId, memberIds)
            createChatRoomInTx(accountId, memberIds)
        }
    }

    @Transactional
    fun readMessages(accountId: Long, roomId: Long) {
        val member = getJoinedMember(roomId, accountId)
        member.resetUnread()
    }

    @Transactional
    fun retryTranslation(accountId: Long, roomId: Long, messageId: Long): SendMessageResult {
        findChatRoomOrThrow(roomId)
        getJoinedMember(roomId, accountId)

        val message = findMessageOrThrow(messageId)

        if (message.chatRoomId != roomId) {
            throw BadRequestException(ErrorCode.CHAT_MESSAGE_ROOM_MISMATCH)
        }
        if (message.senderId == accountId) {
            throw BadRequestException(ErrorCode.CHAT_CANNOT_RETRY_OWN_MESSAGE)
        }
        if (message.translationStatus != TranslationStatus.FAILED) {
            throw BadRequestException(ErrorCode.CHAT_TRANSLATION_NOT_FAILED)
        }

        messageTranslationService.translateMessage(message)

        val saved = chatMessageRepository.save(message)
        return toSendMessageResult(saved, roomId)
    }

    fun validateMembership(roomId: Long, accountId: Long) {
        getJoinedMember(roomId, accountId)
    }

    private fun createChatRoomInTx(accountId: Long, memberIds: List<Long>): ChatRoomResult {
        return txTemplate.execute { _ ->
            if (memberIds.size != 1) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_GROUP_NOT_SUPPORTED)
            }

            val targetId = memberIds.single()
            if (accountId == targetId) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_SELF_CHAT)
            }

            val target = findAccountOrThrow(targetId)
            if (!target.isActive()) {
                throw BadRequestException(ErrorCode.CHAT_ROOM_TARGET_NOT_ACTIVE)
            }

            val existing = chatRoomRepository.findDirectRoom(
                ChatRoom.buildPairKey(accountId, targetId),
            )
            if (existing != null) {
                val members = roomMemberRepository.findByChatRoomId(existing.id)
                return@execute assembleResult(existing, members)
            }

            val requester = findAccountOrThrow(accountId)
            val roomName = "${requester.nickname?.value ?: "User"}, ${target.nickname?.value ?: "User"}"
            val chatRoom = chatRoomRepository.save(
                ChatRoom.createDirect(
                    roomName = roomName,
                    roomOwnerId = accountId,
                    accountId1 = accountId,
                    accountId2 = targetId,
                ),
            )

            val members = roomMemberRepository.saveAll(
                listOf(
                    RoomMember(chatRoomId = chatRoom.id, accountId = accountId),
                    RoomMember(chatRoomId = chatRoom.id, accountId = targetId),
                ),
            )

            log.info("Chat room created: roomId={}, pairKey={}", chatRoom.id, chatRoom.directPairKey)
            assembleResult(chatRoom, members)
        }!!
    }

    private fun assembleResult(chatRoom: ChatRoom, members: List<RoomMember>): ChatRoomResult {
        val accountIds = members.map { it.accountId }
        val accountsById = accountRepository.findAllById(accountIds).associateBy { it.id }
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

    private fun getJoinedMember(roomId: Long, accountId: Long): RoomMember {
        val member = roomMemberRepository.findByChatRoomIdAndAccountId(roomId, accountId)
            ?: throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        if (!member.isJoined()) {
            throw ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)
        }
        return member
    }

    private fun loadRoomMembers(roomIds: List<Long>): Map<Long, List<RoomMember>> {
        if (roomIds.isEmpty()) return emptyMap()
        return roomMemberRepository.findByChatRoomIdIn(roomIds).groupBy { it.chatRoomId }
    }

    private fun loadAccounts(members: List<RoomMember>): Map<Long, Account> {
        val accountIds = members.map { it.accountId }.distinct()
        if (accountIds.isEmpty()) return emptyMap()
        return accountRepository.findAllById(accountIds).associateBy { it.id }
    }

    private fun loadLastMessages(roomIds: List<Long>): Map<Long, ChatMessage> {
        if (roomIds.isEmpty()) return emptyMap()
        return chatMessageRepository.findLatestByRoomIds(roomIds).associateBy { it.chatRoomId }
    }

    private fun buildParticipants(
        members: List<RoomMember>,
        accountsById: Map<Long, Account>,
    ): List<ChatParticipant> = members.map { member ->
        val account = accountsById[member.accountId]
        ChatParticipant(
            userId = member.accountId,
            nickname = account?.nickname?.value,
            profileImageUrl = account?.profileImageUrl,
            nationality = account?.nation?.name,
            unreadCount = member.unreadCount,
        )
    }

    private fun ChatMessage.toSummary() = LastMessageSummary(
        messageId = id,
        senderId = senderId,
        originalContent = originalContent,
        translatedContent = translatedContent,
        translationStatus = translationStatus,
        originalLanguage = originalLanguage,
        translatedLanguage = translatedLanguage,
        createdAt = createdAt,
    )

    private fun toSendMessageResult(message: ChatMessage, roomId: Long) = SendMessageResult(
        messageId = message.id,
        chatRoomId = roomId,
        senderId = message.senderId,
        originalContent = message.originalContent,
        messageStatus = message.messageStatus,
        translatedContent = message.translatedContent,
        translationStatus = message.translationStatus,
        originalLanguage = message.originalLanguage,
        translatedLanguage = message.translatedLanguage,
        createdAt = message.createdAt,
    )

    private fun findChatRoomOrThrow(id: Long): ChatRoom =
        chatRoomRepository.findByIdOrNull(id) ?: throw NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)

    private fun findMessageOrThrow(id: Long): ChatMessage =
        chatMessageRepository.findByIdOrNull(id) ?: throw NotFoundException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)

    private fun findAccountOrThrow(id: Long): Account =
        accountRepository.findByIdOrNull(id) ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
}
