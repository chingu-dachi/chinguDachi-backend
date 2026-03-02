package com.chat.chingudachi.domain.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ChatMessageTest : DescribeSpec() {
    init {
        describe("create") {
            it("유효한 입력으로 메시지를 생성한다") {
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, content = "Hello!")
                message.chatRoomId shouldBe 1L
                message.senderId shouldBe 1L
                message.content shouldBe "Hello!"
                message.messageStatus shouldBe MessageStatus.SENT
            }

            it("빈 문자열이면 예외를 던진다") {
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(chatRoomId = 1L, senderId = 1L, content = "")
                }
            }

            it("공백만 있으면 예외를 던진다") {
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(chatRoomId = 1L, senderId = 1L, content = "   ")
                }
            }

            it("1000자 초과이면 예외를 던진다") {
                val longContent = "a".repeat(1001)
                shouldThrow<IllegalArgumentException> {
                    ChatMessage.create(chatRoomId = 1L, senderId = 1L, content = longContent)
                }
            }

            it("1000자 이하이면 정상 생성된다") {
                val maxContent = "a".repeat(1000)
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, content = maxContent)
                message.content.length shouldBe 1000
            }
        }
    }
}
