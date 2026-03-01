package com.chat.chingudachi.domain.account

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NationTest : DescribeSpec() {
    init {
        describe("toNativeLanguage") {
            it("각 Nation에 맞춘 Language를 제공한다.") {
                Nation.KR.toNativeLanguage() shouldBe NativeLanguage.KO
                Nation.JP.toNativeLanguage() shouldBe NativeLanguage.JA
            }
        }
    }
}
