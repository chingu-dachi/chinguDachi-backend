package com.chat.chingudachi.chat.domain

import com.chat.chingudachi.account.domain.TranslateLanguage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ChatMessageTranslationTest : DescribeSpec() {
    init {
        describe("applyTranslation") {
            it("status를 DONE으로, 번역문을 저장한다") {
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, originalContent = "안녕하세요")

                message.applyTranslation("こんにちは", TranslateLanguage.JA)

                message.translationStatus shouldBe TranslationStatus.DONE
                message.translatedContent shouldBe "こんにちは"
                message.translatedLanguage shouldBe "ja"
            }

            it("KO 타겟으로 번역 시 translatedLanguage가 ko로 설정된다") {
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, originalContent = "こんにちは")

                message.applyTranslation("안녕하세요", TranslateLanguage.KO)

                message.translationStatus shouldBe TranslationStatus.DONE
                message.translatedContent shouldBe "안녕하세요"
                message.translatedLanguage shouldBe "ko"
            }
        }

        describe("markTranslationFailed") {
            it("status를 FAILED로 변경한다") {
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, originalContent = "안녕하세요")

                message.markTranslationFailed()

                message.translationStatus shouldBe TranslationStatus.FAILED
                message.translatedContent shouldBe null
            }
        }

        describe("기본 상태") {
            it("생성 시 translationStatus는 NONE이다") {
                val message = ChatMessage.create(chatRoomId = 1L, senderId = 1L, originalContent = "hello")

                message.translationStatus shouldBe TranslationStatus.NONE
                message.translatedContent shouldBe null
                message.originalLanguage shouldBe null
                message.translatedLanguage shouldBe null
            }
        }
    }
}
