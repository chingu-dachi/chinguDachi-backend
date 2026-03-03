package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.chat.port.TranslationPort
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.chat.ChatMessage
import com.chat.chingudachi.domain.chat.TranslationStatus
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SendMessageUseCaseTranslationTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val translationPort = mockk<TranslationPort>()
    private val accountStore = mockk<AccountStore>()
    private val useCase = SendMessageUseCase(chatRoomStore, roomMemberStore, chatMessageStore, translationPort, accountStore)

    init {
        afterEach { clearMocks(chatRoomStore, roomMemberStore, chatMessageStore, translationPort, accountStore) }

        describe("execute - 번역 통합") {
            val myId = 1L
            val otherId = 2L
            val roomId = 10L

            context("번역 성공 시") {
                it("translationStatus가 DONE이고 translatedContent가 포함된 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId)
                    val sender = AccountFixture.create(
                        id = myId,
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { accountStore.getById(myId) } returns sender
                    every { translationPort.translate("안녕하세요", "kr", "ja") } returns "こんにちは"
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } answers { messageSlot.captured }
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = useCase.execute(myId, roomId, "안녕하세요")

                    result.translationStatus shouldBe TranslationStatus.DONE
                    result.translatedContent shouldBe "こんにちは"
                    result.sourceLanguage shouldBe "kr"
                    result.targetLanguage shouldBe "ja"
                    verify(exactly = 1) { translationPort.translate("안녕하세요", "kr", "ja") }
                }
            }

            context("번역 실패 시") {
                it("translationStatus가 FAILED이고 translatedContent는 null이며 메시지는 정상 저장된다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId)
                    val sender = AccountFixture.create(
                        id = myId,
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { accountStore.getById(myId) } returns sender
                    every { translationPort.translate(any(), any(), any()) } throws RuntimeException("API error")
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } answers { messageSlot.captured }
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = useCase.execute(myId, roomId, "안녕하세요")

                    result.translationStatus shouldBe TranslationStatus.FAILED
                    result.translatedContent shouldBe null
                    result.content shouldBe "안녕하세요"
                    verify(exactly = 1) { chatMessageStore.save(any()) }
                }
            }

            context("동일 언어 유저 간 채팅 (translateLanguage == null)") {
                it("translationStatus가 NONE이고 번역을 호출하지 않는다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId)
                    val sender = AccountFixture.create(id = myId, translateLanguage = null)

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { accountStore.getById(myId) } returns sender
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } answers { messageSlot.captured }
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = useCase.execute(myId, roomId, "hello")

                    result.translationStatus shouldBe TranslationStatus.NONE
                    result.translatedContent shouldBe null
                    result.sourceLanguage shouldBe null
                    result.targetLanguage shouldBe null
                    verify(exactly = 0) { translationPort.translate(any(), any(), any()) }
                }
            }

            context("일본어 유저가 한국어로 번역 시") {
                it("sourceLanguage가 jp이고 targetLanguage가 ko인 결과를 반환한다") {
                    val room = ChatRoomFixture.create(id = roomId)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId)
                    val sender = AccountFixture.create(
                        id = myId,
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )

                    every { chatRoomStore.getById(roomId) } returns room
                    every { roomMemberStore.findByRoomIdAndAccountId(roomId, myId) } returns myMember
                    every { accountStore.getById(myId) } returns sender
                    every { translationPort.translate("こんにちは", "jp", "ko") } returns "안녕하세요"
                    val messageSlot = slot<ChatMessage>()
                    every { chatMessageStore.save(capture(messageSlot)) } answers { messageSlot.captured }
                    every { roomMemberStore.incrementUnreadForOtherMembers(roomId, myId) } returns Unit

                    val result = useCase.execute(myId, roomId, "こんにちは")

                    result.translationStatus shouldBe TranslationStatus.DONE
                    result.translatedContent shouldBe "안녕하세요"
                    result.sourceLanguage shouldBe "jp"
                    result.targetLanguage shouldBe "ko"
                }
            }
        }
    }
}
