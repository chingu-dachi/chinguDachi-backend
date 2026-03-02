package com.chat.chingudachi.application.chat

import com.chat.chingudachi.application.chat.port.ChatRoomStore
import com.chat.chingudachi.application.chat.port.RoomMemberStore
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.chat.RoomMember
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.fixture.ChatRoomFixture
import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

class CreateChatRoomUseCaseTest : DescribeSpec() {
    private val chatRoomStore = mockk<ChatRoomStore>()
    private val roomMemberStore = mockk<RoomMemberStore>()
    private val accountStore = mockk<AccountStore>()
    private val txTemplate = mockk<TransactionTemplate>()
    private val useCase = CreateChatRoomUseCase(chatRoomStore, roomMemberStore, accountStore, txTemplate)

    init {
        beforeEach {
            every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
                val callback = firstArg<TransactionCallback<*>>()
                callback.doInTransaction(mockk())
            }
        }

        afterEach { clearMocks(chatRoomStore, roomMemberStore, accountStore, txTemplate) }

        describe("execute") {
            val myId = 1L
            val targetId = 2L

            context("그룹 채팅 시도 (memberIds.size != 1)") {
                it("CHAT_ROOM_GROUP_NOT_SUPPORTED 예외를 던진다") {
                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, listOf(targetId, 3L))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_GROUP_NOT_SUPPORTED
                }
            }

            context("자기 자신에게 채팅방 생성 시도") {
                it("CHAT_ROOM_SELF_CHAT 예외를 던진다") {
                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, listOf(myId))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_SELF_CHAT
                }
            }

            context("비활성 유저에게 채팅방 생성 시도") {
                it("CHAT_ROOM_TARGET_NOT_ACTIVE 예외를 던진다") {
                    val inactiveUser = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.INACTIVE,
                    )
                    every { accountStore.getById(targetId) } returns inactiveUser

                    val exception = shouldThrow<BadRequestException> {
                        useCase.execute(myId, listOf(targetId))
                    }
                    exception.errorCode shouldBe ErrorCode.CHAT_ROOM_TARGET_NOT_ACTIVE
                }
            }

            context("이미 존재하는 채팅방이 있을 때") {
                it("기존 채팅방을 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val existingRoom = ChatRoomFixture.create(
                        id = 10L,
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val members = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoom = existingRoom, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoom = existingRoom, accountId = targetId),
                    )

                    every { accountStore.getById(targetId) } returns target
                    every { chatRoomStore.findDirectRoom(myId, targetId) } returns existingRoom
                    every { roomMemberStore.findByRoomId(10L) } returns members
                    every { accountStore.findByIds(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = useCase.execute(myId, listOf(targetId))

                    result.roomId shouldBe 10L
                    result.participants shouldHaveSize 2
                    verify(exactly = 0) { chatRoomStore.save(any()) }
                }
            }

            context("새로운 채팅방 생성") {
                it("채팅방과 멤버 2명을 생성하고 결과를 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val savedRoom = ChatRoomFixture.create(
                        id = 100L,
                        roomName = "나유저, 타겟유저",
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val savedMembers = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoom = savedRoom, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoom = savedRoom, accountId = targetId),
                    )

                    every { accountStore.getById(targetId) } returns target
                    every { chatRoomStore.findDirectRoom(myId, targetId) } returns null
                    every { accountStore.getById(myId) } returns me
                    every { chatRoomStore.save(any()) } returns savedRoom
                    val membersSlot = slot<List<RoomMember>>()
                    every { roomMemberStore.saveAll(capture(membersSlot)) } returns savedMembers
                    every { accountStore.findByIds(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = useCase.execute(myId, listOf(targetId))

                    result.roomId shouldBe 100L
                    result.roomName shouldBe "나유저, 타겟유저"
                    result.participants shouldHaveSize 2
                    membersSlot.captured shouldHaveSize 2
                }
            }

            context("레이스 컨디션 — DataIntegrityViolationException 발생 시") {
                it("재시도하여 기존 채팅방을 반환한다") {
                    val target = AccountFixture.create(
                        id = targetId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("타겟유저"),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )
                    val me = AccountFixture.create(
                        id = myId,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("나유저"),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val existingRoom = ChatRoomFixture.create(
                        id = 10L,
                        roomOwnerId = myId,
                        directPairKey = "$myId:$targetId",
                    )
                    val members = listOf(
                        RoomMemberFixture.create(id = 1L, chatRoom = existingRoom, accountId = myId),
                        RoomMemberFixture.create(id = 2L, chatRoom = existingRoom, accountId = targetId),
                    )

                    // 첫 번째 호출: 트랜잭션에서 DataIntegrityViolationException
                    // 두 번째 호출: 기존 방 반환
                    var callCount = 0
                    every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
                        callCount++
                        if (callCount == 1) {
                            throw DataIntegrityViolationException("unique constraint violation")
                        }
                        val callback = firstArg<TransactionCallback<*>>()
                        callback.doInTransaction(mockk())
                    }

                    every { accountStore.getById(targetId) } returns target
                    every { chatRoomStore.findDirectRoom(myId, targetId) } returns existingRoom
                    every { roomMemberStore.findByRoomId(10L) } returns members
                    every { accountStore.findByIds(listOf(myId, targetId)) } returns listOf(me, target)

                    val result = useCase.execute(myId, listOf(targetId))

                    result.roomId shouldBe 10L
                    result.participants shouldHaveSize 2
                }
            }
        }
    }
}
