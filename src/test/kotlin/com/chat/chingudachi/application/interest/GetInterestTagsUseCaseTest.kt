package com.chat.chingudachi.application.interest

import com.chat.chingudachi.application.user.port.InterestTagStore
import com.chat.chingudachi.domain.interest.InterestTag
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetInterestTagsUseCaseTest : DescribeSpec() {
    private val interestTagStore = mockk<InterestTagStore>()
    private val useCase = GetInterestTagsUseCase(interestTagStore)

    init {
        describe("execute") {
            context("태그가 존재하면") {
                it("displayOrder 순으로 반환한다") {
                    val tags = listOf(
                        InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1),
                        InterestTag(id = 2, tagKey = "FOOD", labelKo = "음식", labelJa = "食べ物", displayOrder = 2),
                        InterestTag(id = 3, tagKey = "MUSIC", labelKo = "음악", labelJa = "音楽", displayOrder = 3),
                    )
                    every { interestTagStore.findAllOrderByDisplayOrder() } returns tags

                    val result = useCase.execute()

                    result.size shouldBe 3
                    result[0].tagKey shouldBe "TRAVEL"
                    result[1].tagKey shouldBe "FOOD"
                    result[2].tagKey shouldBe "MUSIC"
                    verify(exactly = 1) { interestTagStore.findAllOrderByDisplayOrder() }
                }
            }

            context("태그가 없으면") {
                it("빈 리스트를 반환한다") {
                    every { interestTagStore.findAllOrderByDisplayOrder() } returns emptyList()

                    val result = useCase.execute()

                    result.shouldBeEmpty()
                }
            }
        }
    }
}
