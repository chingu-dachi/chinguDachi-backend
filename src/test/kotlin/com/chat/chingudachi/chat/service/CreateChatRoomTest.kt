package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.ChatRoom
import com.chat.chingudachi.chat.domain.RoomMember
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

class CreateChatRoomTest : DescribeSpec() {
    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val roomMemberRepository = mockk<RoomMemberRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val chatMessageRepository = mockk<ChatMessageRepository>()
    private val messageTranslationService = mockk<MessageTranslationService>()
    private val txTemplate = mockk<TransactionTemplate>()
    private val chatService = ChatService(
        chatRoomRepository, roomMemberRepository, chatMessageRepository,
        accountRepository, messageTranslationService, txTemplate,
    )

    init {
        beforeEach {
            every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
                val callback = firstArg<TransactionCallback<*>>()
                callback.doInTransaction(mockk())
            }
        }

        afterEach { clearMocks(chatRoomRepository, roomMemberRepository, accountRepository, chatMessageRepository, messageTranslationService, txTemplate) }

        describe("createChatRoom") {
            val myId = 1L
            val targetId = 2L

            context("그룹 채팅 시도 (memberIds.size != 1)") {
                it("CHAT_ROOM_GROUP_NOT_SUPPORTED 예외를 던진다") {
                    val exception = shouldThrow<BadRequestException> {
                        chatService.createChatRoom(myId, listOf(targetId, 3L))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_GROUP_NOT_SUPPORTED
                }
            }

            context("자기 자신에게 채팅방 생성 시도") {
                it("CHAT_ROOM_SELF_CHAT 예외를 던진다") {
                    val exception = shouldThrow<BadRequestException> {
                        chatService.createChatRoom(myId, listOf(myId))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_SELF_CHAT
                }
            }

            context("비활성 유저에게 채팅방 생성 시도") {
                it("CHAT_ROOM_TARGET_NOT_ACTIVE 예외를 던진다") {
                    val inactiveUser = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.INACTIVE,
                    )
                    every { accountRepository.findByIdOrNull(targetId) } returns inactiveUser

                    val exception = shouldThrow<BadRequestException> {
                        chatService.createChatRoom(myId, listOf(targetId))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_TARGET_NOT_ACTIVE
                }
            }

            context("이미 존재하는 채팅방이 있을 때") {
                it("기존 채팅방을 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val existingRoom = ChatRoomFixture.create(
                        id = 10L,
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val members = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoomId = existingRoom.id, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoomId = existingRoom.id, accountId = targetId),
                    )

                    every { accountRepository.findByIdOrNull(targetId) } returns target
                    every { chatRoomRepository.findDirectRoom(ChatRoom.buildPairKey(myId, targetId)) } returns existingRoom
                    every { roomMemberRepository.findByChatRoomId(10L) } returns members
                    every { accountRepository.findAllById(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = chatService.createChatRoom(myId, listOf(targetId))

                    result.roomId shouldBe 10L
                    result.participants shouldHaveSize 2
                    verify(exactly = 0) { chatRoomRepository.save(any()) }
                }
            }

            context("새로운 채팅방 생성") {
                it("채팅방과 멤버 2명을 생성하고 결과를 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val savedRoom = ChatRoomFixture.create(
                        id = 100L,
                        roomName = "나유저, 타겟유저",
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val savedMembers = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoomId = savedRoom.id, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoomId = savedRoom.id, accountId = targetId),
                    )

                    every { accountRepository.findByIdOrNull(targetId) } returns target
                    every { chatRoomRepository.findDirectRoom(ChatRoom.buildPairKey(myId, targetId)) } returns null
                    every { accountRepository.findByIdOrNull(myId) } returns me
                    every { chatRoomRepository.save(any()) } returns savedRoom
                    val membersSlot = slot<List<RoomMember>>()
                    every { roomMemberRepository.saveAll(capture(membersSlot)) } returns savedMembers
                    every { accountRepository.findAllById(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = chatService.createChatRoom(myId, listOf(targetId))

                    result.roomId shouldBe 100L
                    result.roomName shouldBe "나유저, 타겟유저"
                    result.participants shouldHaveSize 2
                    membersSlot.captured shouldHaveSize 2
                }
            }

            context("레이스 컨디션 — DataIntegrityViolationException 발생 시") {
                it("재시도하여 기존 채팅방을 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val existingRoom = ChatRoomFixture.create(
                        id = 10L,
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val members = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoomId = existingRoom.id, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoomId = existingRoom.id, accountId = targetId),
                    )

                    // 첫 번째 호출: 트랜잭션에서 DataIntegrityViolationException
                    // 두 번째 호출: 기존 방 반환
                    var callCount = 0
                    every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
                        callCount++
                        if (callCount == 1) {
                            throw DataIntegrityViolationException("unique constraint violation")
                        }
                        val callback = firstArg<TransactionCallback<*>>()
                        callback.doInTransaction(mockk())
                    }

                    every { accountRepository.findByIdOrNull(targetId) } returns target
                    every { chatRoomRepository.findDirectRoom(ChatRoom.buildPairKey(myId, targetId)) } returns existingRoom
                    every { roomMemberRepository.findByChatRoomId(10L) } returns members
                    every { accountRepository.findAllById(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = chatService.createChatRoom(myId, listOf(targetId))

                    result.roomId shouldBe 10L
                    result.participants shouldHaveSize 2
                }
            }
        }
    }
}
