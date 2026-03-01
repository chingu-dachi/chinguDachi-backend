package com.chat.chingudachi.domain.account

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NationTest : DescribeSpec() {
    init {
        describe("toTranslateLanguage") {
            it("각 Nation에 맞춘 Language를 제공한다.") {
                Nation.KR.toTranslateLanguage() shouldBe TranslateLanguage.JA
                Nation.JP.toTranslateLanguage() shouldBe TranslateLanguage.KO
            }
        }
    }
}
