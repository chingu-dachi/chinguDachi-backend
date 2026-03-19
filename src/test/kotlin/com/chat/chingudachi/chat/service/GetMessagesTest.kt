package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.MemberStatus
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.core.application.PaginationConstants
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate

class GetMessagesTest : DescribeSpec() {
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

        describe("getMessages") {
            val myId = 1L
            val roomId = 10L

            context("멤버가 아닌 유저가 조회 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        chatService.getMessages(myId, roomId, null)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 없을 때") {
                it("빈 목록을 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByRoomId(roomId, null, PageRequest.of(0, PaginationConstants.DEFAULT_SIZE + 1)) } returns emptyList()

                    val result = chatService.getMessages(myId, roomId, null)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("메시지가 DEFAULT_SIZE 이하일 때") {
                it("전체 메시지를 반환하고 hasMore=false") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val messages = (1..5).map { i ->
                        ChatMessageFixture.create(id = i.toLong(), chatRoomId = roomId, senderId = myId, originalContent = "msg $i")
                    }
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByRoomId(roomId, null, PageRequest.of(0, PaginationConstants.DEFAULT_SIZE + 1)) } returns messages

                    val result = chatService.getMessages(myId, roomId, null)

                    result.items shouldHaveSize 5
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("메시지가 DEFAULT_SIZE 초과일 때") {
                it("DEFAULT_SIZE만큼 반환하고 hasMore=true, nextCursor 설정") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val messages = (1..PaginationConstants.DEFAULT_SIZE + 1).map { i ->
                        ChatMessageFixture.create(id = i.toLong(), chatRoomId = roomId, senderId = myId, originalContent = "msg $i")
                    }
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByRoomId(roomId, null, PageRequest.of(0, PaginationConstants.DEFAULT_SIZE + 1)) } returns messages

                    val result = chatService.getMessages(myId, roomId, null)

                    result.items shouldHaveSize PaginationConstants.DEFAULT_SIZE
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe PaginationConstants.DEFAULT_SIZE.toLong()
                }
            }

            context("커서 기반 페이지네이션") {
                it("cursor를 전달하면 해당 cursor로 조회한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val cursor = 50L
                    val messages = (1..3).map { i ->
                        ChatMessageFixture.create(id = (50 - i).toLong(), chatRoomId = roomId, senderId = myId)
                    }
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByRoomId(roomId, cursor, PageRequest.of(0, PaginationConstants.DEFAULT_SIZE + 1)) } returns messages

                    val result = chatService.getMessages(myId, roomId, cursor)

                    result.items shouldHaveSize 3
                    result.hasMore shouldBe false
                }
            }
        }
    }
}
