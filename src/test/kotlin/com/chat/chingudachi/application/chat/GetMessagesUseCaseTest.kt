package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.common.PaginationConstants
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import com.chat.chingudachi.fixture.ChatMessageFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetMessagesUseCaseTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val useCase = GetMessagesUseCase(chatRoomStore, roomMemberStore, chatMessageStore)

    init {
        afterEach { clearMocks(chatRoomStore, roomMemberStore, chatMessageStore) }

        describe("execute") {
            val myId = 1L
            val roomId = 10L

            context("멤버가 아닌 유저가 조회 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        useCase.execute(myId, roomId, null)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 없을 때") {
                it("빈 목록을 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.findByRoomId(roomId, null, PaginationConstants.DEFAULT_SIZE + 1) } returns emptyList()

                    val result = useCase.execute(myId, roomId, null)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("메시지가 DEFAULT_SIZE 이하일 때") {
                it("전체 메시지를 반환하고 hasMore=false") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val messages = (1..5).map { i ->
                        ChatMessageFixture.create(id = i.toLong(), chatRoomId = roomId, senderId = myId, content = "msg $i")
                    }
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.findByRoomId(roomId, null, PaginationConstants.DEFAULT_SIZE + 1) } returns messages

                    val result = useCase.execute(myId, roomId, null)

                    result.items shouldHaveSize 5
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("메시지가 DEFAULT_SIZE 초과일 때") {
                it("DEFAULT_SIZE만큼 반환하고 hasMore=true, nextCursor 설정") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val messages = (1..PaginationConstants.DEFAULT_SIZE + 1).map { i ->
                        ChatMessageFixture.create(id = i.toLong(), chatRoomId = roomId, senderId = myId, content = "msg $i")
                    }
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.findByRoomId(roomId, null, PaginationConstants.DEFAULT_SIZE + 1) } returns messages

                    val result = useCase.execute(myId, roomId, null)

                    result.items shouldHaveSize PaginationConstants.DEFAULT_SIZE
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe PaginationConstants.DEFAULT_SIZE.toLong()
                }
            }

            context("커서 기반 페이지네이션") {
                it("cursor를 전달하면 해당 cursor로 조회한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val cursor = 50L
                    val messages = (1..3).map { i ->
                        ChatMessageFixture.create(id = (50 - i).toLong(), chatRoomId = roomId, senderId = myId)
                    }
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.findByRoomId(roomId, cursor, PaginationConstants.DEFAULT_SIZE + 1) } returns messages

                    val result = useCase.execute(myId, roomId, cursor)

                    result.items shouldHaveSize 3
                    result.hasMore shouldBe false
                }
            }
        }
    }
}
