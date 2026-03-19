package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.chat.domain.RoomStatus
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.fixture.ChatMessageFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate

class SendMessageTest : DescribeSpec() {
    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val roomMemberRepository = mockk<RoomMemberRepository>()
    private val chatMessageRepository = mockk<ChatMessageRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val messageTranslationService = mockk<MessageTranslationService>()
    private val txTemplate = mockk<TransactionTemplate>()
    private val chatService = ChatService(
        chatRoomRepository, roomMemberRepository, chatMessageRepository,
        accountRepository, messageTranslationService, txTemplate,
    )

    init {
        afterEach { clearMocks(chatRoomRepository, roomMemberRepository, chatMessageRepository, accountRepository, messageTranslationService, txTemplate) }

        describe("sendMessage") {
            val myId = 1L
            val otherId = 2L
            val roomId = 10L

            context("방이 존재하지 않을 때") {
                it("CHAT_ROOM_NOT_FOUND 예외를 던진다") {
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns null

                    shouldThrow<NotFoundException> {
                        chatService.sendMessage(myId, roomId, "hello")
                    }
                }
            }

            context("비활성 방에 메시지 전송 시도") {
                it("CHAT_ROOM_NOT_ACTIVE 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId, roomStatus = RoomStatus.ARCHIVED)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room

                    val exception = shouldThrow<BadRequestException> {
                        chatService.sendMessage(myId, roomId, "hello")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_NOT_ACTIVE
                }
            }

            context("멤버가 아닌 유저가 메시지 전송 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        chatService.sendMessage(myId, roomId, "hello")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 1000자 초과일 때") {
                it("CHAT_MESSAGE_TOO_LONG 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<BadRequestException> {
                        chatService.sendMessage(myId, roomId, "a".repeat(1001))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_TOO_LONG
                }
            }

            context("빈 메시지 전송 시도") {
                it("CHAT_MESSAGE_EMPTY 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<BadRequestException> {
                        chatService.sendMessage(myId, roomId, "   ")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_EMPTY
                }
            }

            context("정상 메시지 전송") {
                it("메시지를 저장하고 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoomId = room.id, accountId = myId)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId, originalContent = "hello")

                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { messageTranslationService.translateMessage(any()) } just Runs
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageRepository.save(capture(messageSlot)) } returns savedMessage
                    every { roomMemberRepository.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = chatService.sendMessage(myId, roomId, "hello")

                    result.messageId shouldBe 100L
                    result.chatRoomId shouldBe roomId
                    result.senderId shouldBe myId
                    result.originalContent shouldBe "hello"
                    room.lastMessageAt shouldBe result.createdAt.let { room.lastMessageAt }
                }

                it("상대방의 unreadCount를 배치로 증가시킨다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoomId = room.id, accountId = myId)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId)

                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { messageTranslationService.translateMessage(any()) } just Runs
                    every { chatMessageRepository.save(any()) } returns savedMessage
                    every { roomMemberRepository.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    chatService.sendMessage(myId, roomId, "hello")

                    verify { roomMemberRepository.incrementUnreadForOtherMembers(roomId, myId) }
                }
            }
        }
    }
}
