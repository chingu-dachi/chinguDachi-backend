package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatMessageStore
import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.domain.ChatMessage
import com.chat.chingudachi.chat.domain.RoomStatus
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
import com.chat.chingudachi.fixture.ChatMessageFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify

class SendMessageUseCaseTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val messageTranslationService = mockk<MessageTranslationService>()
    private val useCase = SendMessageUseCase(chatRoomStore, roomMemberStore, chatMessageStore, messageTranslationService)

    init {
        afterEach { clearMocks(chatRoomStore, roomMemberStore, chatMessageStore, messageTranslationService) }

        describe("execute") {
            val myId = 1L
            val otherId = 2L
            val roomId = 10L

            context("방이 존재하지 않을 때") {
                it("CHAT_ROOM_NOT_FOUND 예외를 던진다") {
                    every { chatRoomStore.getById(roomId) } throws
                        com.chat.chingudachi.core.domain.NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)

                    shouldThrow<com.chat.chingudachi.core.domain.NotFoundException> {
                        useCase.execute(myId, roomId, "hello")
                    }
                }
            }

            context("비활성 방에 메시지 전송 시도") {
                it("CHAT_ROOM_NOT_ACTIVE 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId, roomStatus = RoomStatus.ARCHIVED)
                    every { chatRoomStore.getById(roomId) } returns room

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, "hello")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_NOT_ACTIVE
                }
            }

            context("멤버가 아닌 유저가 메시지 전송 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        useCase.execute(myId, roomId, "hello")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 1000자 초과일 때") {
                it("CHAT_MESSAGE_TOO_LONG 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, "a".repeat(1001))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_TOO_LONG
                }
            }

            context("빈 메시지 전송 시도") {
                it("CHAT_MESSAGE_EMPTY 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, "   ")
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_EMPTY
                }
            }

            context("정상 메시지 전송") {
                it("메시지를 저장하고 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId, originalContent = "hello")

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { messageTranslationService.translateMessage(any()) } just Runs
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } returns savedMessage
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = useCase.execute(myId, roomId, "hello")

                    result.messageId shouldBe 100L
                    result.chatRoomId shouldBe roomId
                    result.senderId shouldBe myId
                    result.originalContent shouldBe "hello"
                    room.lastMessageAt shouldBe result.createdAt.let { room.lastMessageAt }
                }

                it("상대방의 unreadCount를 배치로 증가시킨다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId)

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { messageTranslationService.translateMessage(any()) } just Runs
                    every { chatMessageStore.save(any()) } returns savedMessage
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    useCase.execute(myId, roomId, "hello")

                    verify { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) }
                }
            }
        }
    }
}
