package com.chat.chingudachi.chat.application

import com.chat.chingudachi.chat.application.port.ChatMessageStore
import com.chat.chingudachi.chat.application.port.ChatRoomStore
import com.chat.chingudachi.chat.application.port.RoomMemberStore
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.chat.domain.MessageStatus
import com.chat.chingudachi.chat.domain.TranslationStatus
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
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs

class RetryTranslationUseCaseTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val messageTranslationService = mockk<MessageTranslationService>()
    private val useCase = RetryTranslationUseCase(chatRoomStore, roomMemberStore, chatMessageStore, messageTranslationService)

    init {
        afterEach { clearMocks(chatRoomStore, roomMemberStore, chatMessageStore, messageTranslationService) }

        describe("execute") {
            val myId = 1L
            val senderId = 2L
            val roomId = 10L
            val messageId = 100L

            context("방이 존재하지 않을 때") {
                it("CHAT_ROOM_NOT_FOUND 예외를 던진다") {
                    every { chatRoomStore.getById(roomId) } throws
                        NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND)

                    shouldThrow<NotFoundException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                }
            }

            context("멤버가 아닌 유저가 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 존재하지 않을 때") {
                it("CHAT_MESSAGE_NOT_FOUND 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } throws
                        NotFoundException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)

                    shouldThrow<NotFoundException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                }
            }

            context("메시지가 다른 방의 메시지일 때") {
                it("CHAT_MESSAGE_ROOM_MISMATCH 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = 999L,
                        senderId = senderId,
                        translationStatus = TranslationStatus.FAILED,
                    )
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_ROOM_MISMATCH
                }
            }

            context("자신이 보낸 메시지에 번역 재시도 시도") {
                it("CHAT_CANNOT_RETRY_OWN_MESSAGE 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = myId,
                        translationStatus = TranslationStatus.FAILED,
                    )
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_CANNOT_RETRY_OWN_MESSAGE
                }
            }

            context("번역이 FAILED 상태가 아닐 때") {
                it("CHAT_TRANSLATION_NOT_FAILED 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        translationStatus = TranslationStatus.DONE,
                    )
                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_TRANSLATION_NOT_FAILED
                }
            }

            context("번역 재시도 성공") {
                it("번역 결과를 반영하고 DONE 상태로 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        originalContent = "안녕하세요",
                        translationStatus = TranslationStatus.FAILED,
                    )

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } returns message
                    every { messageTranslationService.translateMessage(message) } answers {
                        firstArg<com.chat.chingudachi.chat.domain.ChatMessage>()
                            .applyTranslation("こんにちは", TranslateLanguage.JA)
                    }
                    every { chatMessageStore.save(message) } returns message

                    val result = useCase.execute(myId, roomId, messageId)

                    result.messageId shouldBe messageId
                    result.translatedContent shouldBe "こんにちは"
                    result.translationStatus shouldBe TranslationStatus.DONE
                    result.messageStatus shouldBe MessageStatus.SENT
                }
            }

            context("번역 재시도 실패") {
                it("FAILED 상태로 유지하고 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        originalContent = "안녕하세요",
                        translationStatus = TranslationStatus.FAILED,
                    )

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageStore.getById(messageId) } returns message
                    every { messageTranslationService.translateMessage(message) } answers {
                        firstArg<com.chat.chingudachi.chat.domain.ChatMessage>().markTranslationFailed()
                    }
                    every { chatMessageStore.save(message) } returns message

                    val result = useCase.execute(myId, roomId, messageId)

                    result.translationStatus shouldBe TranslationStatus.FAILED
                    result.translatedContent shouldBe null
                }
            }
        }
    }
}
