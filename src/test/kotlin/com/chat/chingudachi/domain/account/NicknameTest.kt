package com.chat.chingudachi.domain.account

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec

class NicknameTest : DescribeSpec() {
    init {
        describe("Nickname") {
            context("nickname 초기화 시 ") {
                it("2~12자 이내면 정상적으로 Nickname 객체가 생성된다.") {
                    shouldNotThrowAny { Nickname("te") }
                    shouldNotThrowAny { Nickname("test") }
                    shouldNotThrowAny { Nickname("testtesttest") }
                }

                it("한글/일본어 닉네임이 정상적으로 생성된다.") {
                    shouldNotThrowAny { Nickname("친구다치") }
                    shouldNotThrowAny { Nickname("ともだち") }
                    shouldNotThrowAny { Nickname("한일친구") }
                }

                it("2자 미만이나 12자 초과, 공백포함 시 예외가 발생한다.") {
                    shouldThrow<IllegalArgumentException> { Nickname("t") }
                    shouldThrow<IllegalArgumentException> { Nickname("te st") }
                    shouldThrow<IllegalArgumentException> { Nickname("testtesttestt") }
                }
            }
        }
    }
}
