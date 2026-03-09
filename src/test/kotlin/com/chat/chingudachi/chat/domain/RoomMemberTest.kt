package com.chat.chingudachi.chat.domain

import com.chat.chingudachi.fixture.RoomMemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class RoomMemberTest : DescribeSpec() {
    init {
        describe("incrementUnread") {
            it("unreadCount를 1 증가시킨다") {
                val member = RoomMemberFixture.create(unreadCount = 0)
                member.incrementUnread()
                member.unreadCount shouldBe 1
            }

            it("여러 번 호출하면 누적된다") {
                val member = RoomMemberFixture.create(unreadCount = 5)
                member.incrementUnread()
                member.incrementUnread()
                member.unreadCount shouldBe 7
            }
        }

        describe("resetUnread") {
            it("unreadCount를 0으로 초기화하고 lastReadAt을 설정한다") {
                val member = RoomMemberFixture.create(unreadCount = 10)
                member.resetUnread()
                member.unreadCount shouldBe 0
                member.lastReadAt.shouldNotBeNull()
            }
        }

        describe("isJoined") {
            it("JOINED 상태이면 true를 반환한다") {
                val member = RoomMemberFixture.create(memberStatus = MemberStatus.JOINED)
                member.isJoined() shouldBe true
            }

            it("LEFT 상태이면 false를 반환한다") {
                val member = RoomMemberFixture.create(memberStatus = MemberStatus.LEFT)
                member.isJoined() shouldBe false
            }
        }
    }
}
