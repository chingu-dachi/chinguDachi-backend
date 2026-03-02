package com.chat.chingudachi.presentation.chat

import com.chat.chingudachi.application.chat.ReadMessagesUseCase
import com.chat.chingudachi.application.chat.SendMessageUseCase
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.result.SendMessageResult
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.ForbiddenException
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.security.Principal
import java.time.Instant

class ChatMessageHandlerTest : DescribeSpec() {
    private val sendMessageUseCase = mockk<SendMessageUseCase>()
    private val readMessagesUseCase = mockk<ReadMessagesUseCase>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val messagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
    private val handler = ChatMessageHandler(
        sendMessageUseCase,
        readMessagesUseCase,
        roomMemberStore,
        messagingTemplate,
    )

    private val principal = mockk<Principal> {
        every { name } returns "1"
    }

    init {
        afterEach { clearMocks(sendMessageUseCase, readMessagesUseCase, roomMemberStore, messagingTemplate) }

        describe("handleSend") {
            val payload = SendMessagePayload(roomId = 10L, text = "hello")
            val now = Instant.now()

            context("정상 메시지 전송") {
                it("채팅방에 broadcast하고 발신자에게 ACK을 전송한다") {
                    val result = SendMessageResult(
                        messageId = 100L,
                        chatRoomId = 10L,
                        senderId = 1L,
                        content = "hello",
                        createdAt = now,
                    )
                    every { sendMessageUseCase.execute(1L, 10L, "hello") } returns result

                    handler.handleSend(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<ChatEvent>()) }
                    verify { messagingTemplate.convertAndSendToUser("1", "/queue/chat.ack", any<MessageAck>()) }
                }
            }
        }

        describe("handleTypingStart") {
            val payload = TypingPayload(roomId = 10L)

            context("멤버인 유저가 타이핑") {
                it("채팅방에 TYPING_START 이벤트를 broadcast한다") {
                    val room = ChatRoomFixture.create(id = 10L)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = 1L)
                    every { roomMemberStore.findByRoomIdAndAccountId(10L, 1L) } returns member

                    handler.handleTypingStart(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<ChatEvent>()) }
                }
            }

            context("멤버가 아닌 유저가 타이핑") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    every { roomMemberStore.findByRoomIdAndAccountId(10L, 1L) } returns null

                    val exception = runCatching { handler.handleTypingStart(payload, principal) }
                        .exceptionOrNull() as ForbiddenException

                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }
        }

        describe("handleTypingStop") {
            val payload = TypingPayload(roomId = 10L)

            context("멤버인 유저가 타이핑 중지") {
                it("채팅방에 TYPING_STOP 이벤트를 broadcast한다") {
                    val room = ChatRoomFixture.create(id = 10L)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = 1L)
                    every { roomMemberStore.findByRoomIdAndAccountId(10L, 1L) } returns member

                    handler.handleTypingStop(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<ChatEvent>()) }
                }
            }
        }

        describe("handleRead") {
            val payload = ReadPayload(roomId = 10L)

            context("정상 읽음 처리") {
                it("읽음 처리 후 READ 이벤트를 broadcast한다") {
                    justRun { readMessagesUseCase.execute(1L, 10L) }

                    handler.handleRead(payload, principal)

                    verify { readMessagesUseCase.execute(1L, 10L) }
                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<ChatEvent>()) }
                }
            }
        }

        describe("handleBusinessException") {
            it("에러 이벤트를 반환한다") {
                val ex = BadRequestException(ErrorCode.CHAT_MESSAGE_EMPTY)

                val result = handler.handleBusinessException(ex)

                result.code shouldBe "CHAT_MESSAGE_EMPTY"
                result.message shouldBe "Message must not be empty"
            }
        }

        describe("handleException") {
            it("INTERNAL_ERROR 이벤트를 반환한다") {
                val ex = RuntimeException("unexpected")

                val result = handler.handleException(ex)

                result.code shouldBe "INTERNAL_ERROR"
            }
        }
    }
}
