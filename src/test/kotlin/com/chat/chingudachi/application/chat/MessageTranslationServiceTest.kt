package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.TranslationStatus
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatMessageFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MessageTranslationServiceTest : DescribeSpec() {
    private val translationPort = mockk<TranslationPort>()
    private val accountStore = mockk<AccountStore>()
    private val service = MessageTranslationService(translationPort, accountStore)

    init {
        afterEach { clearMocks(translationPort, accountStore) }

        describe("translateMessage") {
            val senderId = 1L

            context("번역 성공 시") {
                it("translationStatus가 DONE이고 translatedContent가 설정된다") {
                    val message = ChatMessageFixture.create(senderId = senderId, originalContent = "안녕하세요")
                    val sender = AccountFixture.create(
                        id = senderId,
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { accountStore.getById(senderId) } returns sender
                    every { translationPort.translate("안녕하세요", "kr", "ja") } returns "こんにちは"

                    service.translateMessage(message)

                    message.translationStatus shouldBe TranslationStatus.DONE
                    message.translatedContent shouldBe "こんにちは"
                    message.originalLanguage shouldBe "kr"
                    message.translatedLanguage shouldBe "ja"
                    verify(exactly = 1) { translationPort.translate("안녕하세요", "kr", "ja") }
                }
            }

            context("번역 실패 시") {
                it("translationStatus가 FAILED이고 translatedContent는 null이다") {
                    val message = ChatMessageFixture.create(senderId = senderId, originalContent = "안녕하세요")
                    val sender = AccountFixture.create(
                        id = senderId,
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { accountStore.getById(senderId) } returns sender
                    every { translationPort.translate(any(), any(), any()) } throws RuntimeException("API error")

                    service.translateMessage(message)

                    message.translationStatus shouldBe TranslationStatus.FAILED
                    message.translatedContent shouldBe null
                    message.originalLanguage shouldBe "kr"
                }
            }

            context("동일 언어 유저 (translateLanguage == null)") {
                it("번역을 호출하지 않고 상태가 변경되지 않는다") {
                    val message = ChatMessageFixture.create(senderId = senderId, originalContent = "hello")
                    val sender = AccountFixture.create(id = senderId, translateLanguage = null)

                    every { accountStore.getById(senderId) } returns sender

                    service.translateMessage(message)

                    message.translationStatus shouldBe TranslationStatus.NONE
                    message.translatedContent shouldBe null
                    message.originalLanguage shouldBe null
                    verify(exactly = 0) { translationPort.translate(any(), any(), any()) }
                }
            }

            context("일본어 유저가 한국어로 번역 시") {
                it("sourceLanguage가 jp이고 targetLanguage가 ko인 결과를 반환한다") {
                    val message = ChatMessageFixture.create(senderId = senderId, originalContent = "こんにちは")
                    val sender = AccountFixture.create(
                        id = senderId,
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )

                    every { accountStore.getById(senderId) } returns sender
                    every { translationPort.translate("こんにちは", "jp", "ko") } returns "안녕하세요"

                    service.translateMessage(message)

                    message.translationStatus shouldBe TranslationStatus.DONE
                    message.translatedContent shouldBe "안녕하세요"
                    message.originalLanguage shouldBe "jp"
                    message.translatedLanguage shouldBe "ko"
                }
            }
        }
    }
}
