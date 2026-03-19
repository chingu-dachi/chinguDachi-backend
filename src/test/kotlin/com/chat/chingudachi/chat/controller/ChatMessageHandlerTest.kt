package com.chat.chingudachi.chat.controller

import com.chat.chingudachi.chat.domain.MessageStatus
import com.chat.chingudachi.chat.domain.TranslationStatus
import com.chat.chingudachi.chat.service.ChatService
import com.chat.chingudachi.chat.service.SendMessageResult
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.ForbiddenException
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
    private val chatService = mockk<ChatService>()
    private val messagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
    private val handler = ChatMessageHandler(
        chatService,
        messagingTemplate,
    )

    private val principal = mockk<Principal> {
        every { name } returns "1"
    }

    init {
        afterEach { clearMocks(chatService, messagingTemplate) }

        describe("handleSend") {
            val payload = SendMessagePayload(roomId = 10L, text = "hello")
            val now = Instant.now()

            context("정상 메시지 전송") {
                it("채팅방에 broadcast하고 발신자에게 ACK을 전송한다") {
                    val result = SendMessageResult(
                        messageId = 100L,
                        chatRoomId = 10L,
                        senderId = 1L,
                        originalContent = "hello",
                        messageStatus = MessageStatus.SENT,
                        translatedContent = null,
                        translationStatus = TranslationStatus.NONE,
                        originalLanguage = null,
                        translatedLanguage = null,
                        createdAt = now,
                    )
                    every { chatService.sendMessage(1L, 10L, "hello") } returns result

                    handler.handleSend(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<NewMessageEvent>()) }
                    verify { messagingTemplate.convertAndSendToUser("1", "/queue/chat.ack", any<MessageAck>()) }
                }
            }
        }

        describe("handleTypingStart") {
            val payload = TypingPayload(roomId = 10L)

            context("멤버인 유저가 타이핑") {
                it("채팅방에 TYPING_START 이벤트를 broadcast한다") {
                    justRun { chatService.validateMembership(10L, 1L) }

                    handler.handleTypingStart(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<TypingEvent>()) }
                }
            }

            context("멤버가 아닌 유저가 타이핑") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    every { chatService.validateMembership(10L, 1L) } throws ForbiddenException(ErrorCode.CHAT_NOT_A_MEMBER)

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
                    justRun { chatService.validateMembership(10L, 1L) }

                    handler.handleTypingStop(payload, principal)

                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<TypingEvent>()) }
                }
            }
        }

        describe("handleRead") {
            val payload = ReadPayload(roomId = 10L)

            context("정상 읽음 처리") {
                it("읽음 처리 후 READ 이벤트를 broadcast한다") {
                    justRun { chatService.readMessages(1L, 10L) }

                    handler.handleRead(payload, principal)

                    verify { chatService.readMessages(1L, 10L) }
                    verify { messagingTemplate.convertAndSend("/topic/chat/10", any<ReadEvent>()) }
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
