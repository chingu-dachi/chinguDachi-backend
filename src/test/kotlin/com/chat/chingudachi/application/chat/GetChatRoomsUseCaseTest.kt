package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class GetChatRoomsUseCaseTest : DescribeSpec() {
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val accountStore = mockk<AccountStore>()
    private val useCase = GetChatRoomsUseCase(roomMemberStore, accountStore)

    init {
        afterEach { clearMocks(roomMemberStore, accountStore) }

        describe("execute") {
            val myId = 1L
            val otherId = 2L

            context("채팅방이 없을 때") {
                it("빈 리스트와 hasMore=false를 반환한다") {
                    every { roomMemberStore.findByAccountId(myId, null, 21) } returns emptyList()

                    val result = useCase.execute(myId, null, 20)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
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
                    every { roomMemberStore.findByRoomId(10L) } returns listOf(myMember, otherMember)
                    every { accountStore.getById(myId) } returns me
                    every { accountStore.getById(otherId) } returns other

                    val result = useCase.execute(myId, null, 20)

                    result.items shouldHaveSize 1
                    result.items[0].roomId shouldBe 10L
                    result.items[0].participants shouldHaveSize 2
                    result.hasMore shouldBe false
                }
            }

            context("커서 페이지네이션") {
                it("hasMore가 올바르게 설정된다") {
                    val now = Instant.now()
                    val rooms = (1..3).map { i ->
                        ChatRoomFixture.create(
                            id = i.toLong(),
                            lastMessageAt = now.minusSeconds(i.toLong()),
                        )
                    }
                    val members = rooms.map { room ->
                        RoomMemberFixture.create(chatRoom = room, accountId = myId)
                    }
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    every { roomMemberStore.findByAccountId(myId, null, 3) } returns members
                    rooms.forEach { room ->
                        every { roomMemberStore.findByRoomId(room.id) } returns listOf(
                            RoomMemberFixture.create(chatRoom = room, accountId = myId),
                        )
                    }
                    every { accountStore.getById(myId) } returns me

                    val result = useCase.execute(myId, null, 2)

                    result.items shouldHaveSize 2
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe rooms[1].lastMessageAt
                }
            }
        }
    }
}
