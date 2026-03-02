package com.chat.chingudachi.domain.chat

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
    }
}
