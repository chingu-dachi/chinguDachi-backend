package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatMessageStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.chat.TranslationStatus
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
import java.time.Instant

class GetChatRoomsUseCaseTest : DescribeSpec() {
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val accountStore = mockk<AccountStore>()
    private val chatMessageStore = mockk<ChatMessageStore>()
    private val useCase = GetChatRoomsUseCase(roomMemberStore, accountStore, chatMessageStore)

    init {
        afterEach { clearMocks(roomMemberStore, accountStore, chatMessageStore) }

        describe("execute") {
            val myId = 1L
            val otherId = 2L

            context("채팅방이 없을 때") {
                it("빈 리스트와 hasMore=false를 반환한다") {
                    every { roomMemberStore.findByAccountId(myId, null, 21) } returns emptyList()

                    val result = useCase.execute(myId, null)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("채팅방은 있지만 메시지가 없을 때") {
                it("lastMessage가 null이다") {
                    val room = ChatRoomFixture.create(id = 10L, lastMessageAt = null)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { roomMemberStore.findByAccountId(myId, null, 21) } returns listOf(myMember)
                    every { roomMemberStore.findByRoomIds(listOf(10L)) } returns listOf(myMember)
                    every { accountStore.findByIds(listOf(myId)) } returns listOf(me)
                    every { chatMessageStore.findLatestByRoomIds(listOf(10L)) } returns emptyList()

                    val result = useCase.execute(myId, null)

                    result.items shouldHaveSize 1
                    result.items[0].lastMessage.shouldBeNull()
                }
            }

            context("채팅방이 있을 때") {
                it("채팅방 목록을 반환한다") {
                    val now = Instant.now()
                    val room = ChatRoomFixture.create(id = 10L, lastMessageAt = now)
                    val myMember = RoomMemberFixture.create(id = 1L, chatRoom = room, accountId = myId)
                    val otherMember = RoomMemberFixture.create(id = 2L, chatRoom = room, accountId = otherId)
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

                    every { roomMemberStore.findByAccountId(myId, null, 21) } returns listOf(myMember)
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

                    every { roomMemberStore.findByRoomIds(listOf(10L)) } returns listOf(myMember, otherMember)
                    every { accountStore.findByIds(listOf(myId, otherId)) } returns listOf(me, other)
                    every { chatMessageStore.findLatestByRoomIds(listOf(10L)) } returns listOf(lastMsg)

                    val result = useCase.execute(myId, null)

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
                        RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    }
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    val resultRoomIds = rooms.dropLast(1).map { it.id }
                    val resultRoomMembers = rooms.dropLast(1).map { room ->
                        RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    }

                    every { roomMemberStore.findByAccountId(myId, null, 21) } returns myMembers
                    every { roomMemberStore.findByRoomIds(resultRoomIds) } returns resultRoomMembers
                    every { accountStore.findByIds(listOf(myId)) } returns listOf(me)
                    every { chatMessageStore.findLatestByRoomIds(resultRoomIds) } returns emptyList()

                    val result = useCase.execute(myId, null)

                    result.items shouldHaveSize 20
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe resultRoomIds.last()
                    result.items.forEach { it.lastMessage.shouldBeNull() }
                }
            }
        }
    }
}
