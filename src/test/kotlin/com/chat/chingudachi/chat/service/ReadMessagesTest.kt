package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.MemberStatus
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.transaction.support.TransactionTemplate

class ReadMessagesTest : DescribeSpec() {
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

        describe("readMessages") {
            val myId = 1L
            val roomId = 10L

            context("멤버가 아닌 유저가 읽음 처리 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        chatService.readMessages(myId, roomId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("탈퇴한 멤버가 읽음 처리 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(
                        chatRoomId = room.id,
                        accountId = myId,
                        memberStatus = MemberStatus.LEFT,
                    )
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<ForbiddenException> {
                        chatService.readMessages(myId, roomId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("정상 읽음 처리") {
                it("unreadCount를 0으로 초기화하고 lastReadAt을 설정한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(
                        chatRoomId = room.id,
                        accountId = myId,
                        unreadCount = 5,
                    )
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member

                    chatService.readMessages(myId, roomId)

                    member.unreadCount shouldBe 0
                    member.lastReadAt.shouldNotBeNull()
                }
            }
        }
    }
}
