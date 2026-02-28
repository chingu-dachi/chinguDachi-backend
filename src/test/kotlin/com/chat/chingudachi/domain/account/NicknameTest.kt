package com.chat.chingudachi.domain.account

import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class NicknameTest : DescribeSpec() {
    init {
        describe("Nickname") {
            context("nickname 초기화 시 ") {
                it("2~12자 이내면 정상적으로 Nickname 객체가 새성된다.") {
                    val name = "te"
                    val name2 = "test"
                    val name3 = "testtesttest"

                    assertDoesNotThrow { Nickname(name) }
                    assertDoesNotThrow { Nickname(name2) }
                    assertDoesNotThrow { Nickname(name3) }
                }

                it("2자 미만이나 12자 이상, 공백포함 시 예외가 발생한다.") {
                    val name = "t"
                    val name2 = "te st"
                    val name3 = "testtesttestt"

                    assertThrows<IllegalArgumentException> { Nickname(name) }
                    assertThrows<IllegalArgumentException> { Nickname(name2) }
                    assertThrows<IllegalArgumentException> { Nickname(name3) }
                }
            }
        }
    }
}
