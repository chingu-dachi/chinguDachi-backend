package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.MessageStatus
import com.chat.chingudachi.chat.domain.TranslationStatus
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
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate

class RetryTranslationTest : DescribeSpec() {
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

        describe("retryTranslation") {
            val myId = 1L
            val senderId = 2L
            val roomId = 10L
            val messageId = 100L

            context("방이 존재하지 않을 때") {
                it("CHAT_ROOM_NOT_FOUND 예외를 던진다") {
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns null

                    shouldThrow<NotFoundException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                }
            }

            context("멤버가 아닌 유저가 시도") {
                it("CHAT_NOT_A_MEMBER 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns null

                    val exception = shouldThrow<ForbiddenException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_NOT_A_MEMBER
                }
            }

            context("메시지가 존재하지 않을 때") {
                it("CHAT_MESSAGE_NOT_FOUND 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns null

                    shouldThrow<NotFoundException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                }
            }

            context("메시지가 다른 방의 메시지일 때") {
                it("CHAT_MESSAGE_ROOM_MISMATCH 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = 999L,
                        senderId = senderId,
                        translationStatus = TranslationStatus.FAILED,
                    )
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_MESSAGE_ROOM_MISMATCH
                }
            }

            context("자신이 보낸 메시지에 번역 재시도 시도") {
                it("CHAT_CANNOT_RETRY_OWN_MESSAGE 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = myId,
                        translationStatus = TranslationStatus.FAILED,
                    )
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_CANNOT_RETRY_OWN_MESSAGE
                }
            }

            context("번역이 FAILED 상태가 아닐 때") {
                it("CHAT_TRANSLATION_NOT_FAILED 예외를 던진다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        translationStatus = TranslationStatus.DONE,
                    )
                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns message

                    val exception = shouldThrow<BadRequestException> {
                        chatService.retryTranslation(myId, roomId, messageId)
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_TRANSLATION_NOT_FAILED
                }
            }

            context("번역 재시도 성공") {
                it("번역 결과를 반영하고 DONE 상태로 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        originalContent = "안녕하세요",
                        translationStatus = TranslationStatus.FAILED,
                    )

                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns message
                    every { messageTranslationService.translateMessage(message) } answers {
                        firstArg<com.chat.chingudachi.chat.domain.ChatMessage>()
                            .applyTranslation("こんにちは", TranslateLanguage.JA)
                    }
                    every { chatMessageRepository.save(message) } returns message

                    val result = chatService.retryTranslation(myId, roomId, messageId)

                    result.messageId shouldBe messageId
                    result.translatedContent shouldBe "こんにちは"
                    result.translationStatus shouldBe TranslationStatus.DONE
                    result.messageStatus shouldBe MessageStatus.SENT
                }
            }

            context("번역 재시도 실패") {
                it("FAILED 상태로 유지하고 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val member = RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    val message = ChatMessageFixture.create(
                        id = messageId,
                        chatRoomId = roomId,
                        senderId = senderId,
                        originalContent = "안녕하세요",
                        translationStatus = TranslationStatus.FAILED,
                    )

                    every { chatRoomRepository.findByIdOrNull(roomId) } returns room
                    every { roomMemberRepository.findByChatRoomIdAndAccountId(roomId, myId) } returns member
                    every { chatMessageRepository.findByIdOrNull(messageId) } returns message
                    every { messageTranslationService.translateMessage(message) } answers {
                        firstArg<com.chat.chingudachi.chat.domain.ChatMessage>().markTranslationFailed()
                    }
                    every { chatMessageRepository.save(message) } returns message

                    val result = chatService.retryTranslation(myId, roomId, messageId)

                    result.translationStatus shouldBe TranslationStatus.FAILED
                    result.translatedContent shouldBe null
                }
            }
        }
    }
}
