package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.RoomStatus
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import com.chat.chingudachi.fixture.ChatMessageFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SendMessageUseCaseTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val useCase = SendMessageUseCase(chatRoomStore, roomMemberStore, chatMessageStore)

    init {
        afterEach { clearMocks(chatRoomStore, roomMemberStore, chatMessageStore) }

        describe("execute") {
            val myId = 1L
            val otherId = 2L
            val roomId = 10L

            context("방이 존재하지 않을 때") {
                it("CHAT_ROOM_NOT_FOUND 예외를 던진다") {
                    every { chatRoomStore.getById(roomId) } throws
                        com.chat.chingudachi.domain.common.NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)

                    shouldThrow<com.chat.chingudachi.domain.common.NotFoundException> {
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
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId, unreadCount = 0)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId, content = "hello")

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } returns savedMessage
                    every { roomMemberStore.findByRoomId(roomId) } returns listOf(myMember, otherMember)

                    val result = useCase.execute(myId, roomId, "hello")

                    result.messageId shouldBe 100L
                    result.chatRoomId shouldBe roomId
                    result.senderId shouldBe myId
                    result.content shouldBe "hello"
                    otherMember.unreadCount shouldBe 1
                    room.lastMessageAt shouldBe result.createdAt.let { room.lastMessageAt }
                }

                it("상대방의 unreadCount를 증가시킨다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId, unreadCount = 3)
                    val savedMessage = ChatMessageFixture.create(id = 100L, chatRoomId = roomId, senderId = myId)

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { chatMessageStore.save(any()) } returns savedMessage
                    every { roomMemberStore.findByRoomId(roomId) } returns listOf(myMember, otherMember)

                    useCase.execute(myId, roomId, "hello")

                    otherMember.unreadCount shouldBe 4
                    verify { roomMemberStore.findByRoomId(roomId) }
                }
            }
        }
    }
}
