package com.chat.chingudachi.chat.domain

import com.chat.chingudachi.fixture.ChatRoomFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class ChatRoomTest : DescribeSpec() {
    init {
        describe("isActive") {
            it("ACTIVE 상태이면 true를 반환한다") {
                val room = ChatRoomFixture.create(roomStatus = RoomStatus.ACTIVE)
                room.isActive() shouldBe true
            }

            it("ARCHIVED 상태이면 false를 반환한다") {
                val room = ChatRoomFixture.create(roomStatus = RoomStatus.ARCHIVED)
                room.isActive() shouldBe false
            }
        }

        describe("updateLastMessageAt") {
            it("lastMessageAt을 갱신한다") {
                val room = ChatRoomFixture.create()
                val now = Instant.now()
                room.updateLastMessageAt(now)
                room.lastMessageAt shouldBe now
            }
        }

        describe("buildPairKey") {
            it("정렬된 ID 쌍 문자열을 반환한다") {
                ChatRoom.buildPairKey(1L, 42L) shouldBe "1:42"
            }

            it("순서와 무관하게 동일한 키를 생성한다 (대칭성)") {
                val key1 = ChatRoom.buildPairKey(1L, 42L)
                val key2 = ChatRoom.buildPairKey(42L, 1L)
                key1 shouldBe key2
            }
        }

        describe("createDirect") {
            it("directPairKey가 buildPairKey와 동일하다") {
                val room = ChatRoom.createDirect(
                    roomName = "테스트, ユーザー",
                    roomOwnerId = 1L,
                    accountId1 = 42L,
                    accountId2 = 1L,
                )
                room.directPairKey shouldBe ChatRoom.buildPairKey(42L, 1L)
                room.roomType shouldBe RoomType.DIRECT
            }
        }
    }
}
