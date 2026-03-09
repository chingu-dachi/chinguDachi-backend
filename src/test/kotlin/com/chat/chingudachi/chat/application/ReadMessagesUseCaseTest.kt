package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.chat.domain.MemberStatus
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

class ReadMessagesUseCaseTest : DescribeSpec() {
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val useCase = ReadMessagesUseCase(roomMemberStore)

    init {
        afterEach { clearMocks(roomMemberStore) }

        describe("execute") {
            val myId = 1L
            val roomId = 10L

            context("멤버가 아닌 유저가 읽음 처리 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        useCase.execute(myId, roomId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("탈퇴한 멤버가 읽음 처리 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(
                        chatRoom = room,
                        accountId = myId,
                        memberStatus = MemberStatus.LEFT,
                    )
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member

                    val exception = shouldThrow<ForbiddenException> {
                        useCase.execute(myId, roomId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("정상 읽음 처리") {
                it("unreadCount를 0으로 초기화하고 lastReadAt을 설정한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(
                        chatRoom = room,
                        accountId = myId,
                        unreadCount = 5,
                    )
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member

                    useCase.execute(myId, roomId)

                    member.unreadCount shouldBe 0
                    member.lastReadAt.shouldNotBeNull()
                }
            }
        }
    }
}
