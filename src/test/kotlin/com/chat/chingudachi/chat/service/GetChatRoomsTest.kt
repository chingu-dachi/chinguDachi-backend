package com.chat.chingudachi.chat.service

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.chat.domain.TranslationStatus
import com.chat.chingudachi.chat.repository.ChatMessageRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatMessageFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

class GetChatRoomsTest : DescribeSpec() {
    private val chatRoomRepository = mockk<ChatRoomRepository>()
    private val roomMemberRepository = mockk<RoomMemberRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val chatMessageRepository = mockk<ChatMessageRepository>()
    private val messageTranslationService = mockk<MessageTranslationService>()
    private val txTemplate = mockk<TransactionTemplate>()
    private val chatService = ChatService(
        chatRoomRepository, roomMemberRepository, chatMessageRepository,
        accountRepository, messageTranslationService, txTemplate,
    )

    init {
        afterEach { clearMocks(chatRoomRepository, roomMemberRepository, accountRepository, chatMessageRepository, messageTranslationService, txTemplate) }

        describe("getChatRooms") {
            val myId = 1L
            val otherId = 2L

            context("채팅방이 없을 때") {
                it("빈 리스트와 hasMore=false를 반환한다") {
                    every { roomMemberRepository.findJoinedByAccountId(myId, null, PageRequest.of(0, 21)) } returns emptyList()

                    val result = chatService.getChatRooms(myId, null)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("채팅방은 있지만 메시지가 없을 때") {
                it("lastMessage가 null이다") {
                    val room = ChatRoomFixture.create(id = 10L, lastMessageAt = null)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoomId = room.id, accountId = myId)
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { roomMemberRepository.findJoinedByAccountId(myId, null, PageRequest.of(0, 21)) } returns listOf(myMember)
                    every { chatRoomRepository.findAllById(listOf(10L)) } returns listOf(room)
                    every { roomMemberRepository.findByChatRoomIdIn(listOf(10L)) } returns listOf(myMember)
                    every { accountRepository.findAllById(listOf(myId)) } returns listOf(me)
                    every { chatMessageRepository.findLatestByRoomIds(listOf(10L)) } returns emptyList()

                    val result = chatService.getChatRooms(myId, null)

                    result.items shouldHaveSize 1
                    result.items[0].lastMessage.shouldBeNull()
                }
            }

            context("채팅방이 있을 때") {
                it("채팅방 목록을 반환한다") {
                    val now = Instant.now()
                    val room = ChatRoomFixture.create(id = 10L, lastMessageAt = now)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoomId = room.id, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoomId = room.id, accountId = otherId)
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val other = AccountFixture.create(
                        id = otherId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("상대유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )

                    every { roomMemberRepository.findJoinedByAccountId(myId, null, PageRequest.of(0, 21)) } returns listOf(myMember)
                    val lastMsg = ChatMessageFixture.create(
                        id = 100L,
                        chatRoomId = 10L,
                        senderId = otherId,
                        originalContent = "こんにちは",
                        translatedContent = "안녕하세요",
                        translationStatus = TranslationStatus.DONE,
                        originalLanguage = "ja",
                        translatedLanguage = "ko",
                    )

                    every { chatRoomRepository.findAllById(listOf(10L)) } returns listOf(room)
                    every { roomMemberRepository.findByChatRoomIdIn(listOf(10L)) } returns listOf(myMember, otherMember)
                    every { accountRepository.findAllById(listOf(myId, otherId)) } returns listOf(me, other)
                    every { chatMessageRepository.findLatestByRoomIds(listOf(10L)) } returns listOf(lastMsg)

                    val result = chatService.getChatRooms(myId, null)

                    result.items shouldHaveSize 1
                    result.items[0].roomId shouldBe 10L
                    result.items[0].participants shouldHaveSize 2
                    result.hasMore shouldBe false

                    val summary = result.items[0].lastMessage
                    summary.shouldNotBeNull()
                    summary.messageId shouldBe 100L
                    summary.senderId shouldBe otherId
                    summary.originalContent shouldBe "こんにちは"
                    summary.translatedContent shouldBe "안녕하세요"
                    summary.translationStatus shouldBe TranslationStatus.DONE
                }
            }

            context("커서 페이지네이션") {
                it("21개 반환 시 hasMore=true, 20개만 결과에 포함") {
                    val now = Instant.now()
                    val rooms = (1..21).map { i ->
                        ChatRoomFixture.create(
                            id = (22 - i).toLong(),
                            lastMessageAt = now.minusSeconds(i.toLong()),
                        )
                    }
                    val myMembers = rooms.map { room ->
                        RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    }
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    val resultRooms = rooms.dropLast(1)
                    val resultRoomIds = resultRooms.map { it.id }
                    val resultRoomMembers = resultRooms.map { room ->
                        RoomMemberFixture.create(chatRoomId = room.id, accountId = myId)
                    }

                    every { roomMemberRepository.findJoinedByAccountId(myId, null, PageRequest.of(0, 21)) } returns myMembers
                    every { chatRoomRepository.findAllById(resultRoomIds) } returns resultRooms
                    every { roomMemberRepository.findByChatRoomIdIn(resultRoomIds) } returns resultRoomMembers
                    every { accountRepository.findAllById(listOf(myId)) } returns listOf(me)
                    every { chatMessageRepository.findLatestByRoomIds(resultRoomIds) } returns emptyList()

                    val result = chatService.getChatRooms(myId, null)

                    result.items shouldHaveSize 20
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe resultRoomIds.last()
                    result.items.forEach { it.lastMessage.shouldBeNull() }
                }
            }
        }
    }
}
