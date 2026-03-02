package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.application.user.query.DiscoverUsersQuery
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.interest.InterestTag
import com.chat.chingudachi.domain.interest.UserInterest
import com.chat.chingudachi.fixture.AccountFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class DiscoverUsersUseCaseTest : DescribeSpec() {
    private val accountStore = mockk<AccountStore>()
    private val userInterestStore = mockk<UserInterestStore>()
    private val useCase = DiscoverUsersUseCase(accountStore, userInterestStore)

    init {
        afterEach { clearMocks(accountStore, userInterestStore) }

        describe("execute") {
            val currentUserId = 100L

            context("유저 목록 조회") {
                it("자기 자신을 제외하고 ACTIVE 유저를 반환한다") {
                    val user1 = AccountFixture.create(
                        id = 1L,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("유저일"),
                        birthDate = LocalDate.of(1995, 1, 1),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )
                    val user2 = AccountFixture.create(
                        id = 2L,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("유저이"),
                        birthDate = LocalDate.of(1996, 2, 2),
                        nation = Nation.JP,
                        translateLanguage = TranslateLanguage.KO,
                    )

                    val query = DiscoverUsersQuery(currentUserId = currentUserId, size = 20)

                    every {
                        accountStore.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            interests = null,
                            cursor = null,
                            size = 21,
                        )
                    } returns listOf(user1, user2)
                    every { userInterestStore.findByAccountIds(listOf(1L, 2L)) } returns emptyList()

                    val result = useCase.execute(query)

                    result.items shouldHaveSize 2
                    result.items[0].account.id shouldBe 1L
                    result.items[1].account.id shouldBe 2L
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }

            context("커서 페이지네이션") {
                it("hasMore가 올바르게 설정된다") {
                    val users = (1..3).map { i ->
                        AccountFixture.create(
                            id = i.toLong(),
                            accountStatus = AccountStatus.ACTIVE,
                            nickname = Nickname("유저${"가나다라마바사"[i - 1]}"),
                            birthDate = LocalDate.of(1995, 1, 1),
                            nation = Nation.KR,
                            translateLanguage = TranslateLanguage.JA,
                        )
                    }

                    val query = DiscoverUsersQuery(currentUserId = currentUserId, size = 2)

                    every {
                        accountStore.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            interests = null,
                            cursor = null,
                            size = 3,
                        )
                    } returns users
                    every { userInterestStore.findByAccountIds(listOf(1L, 2L)) } returns emptyList()

                    val result = useCase.execute(query)

                    result.items shouldHaveSize 2
                    result.hasMore shouldBe true
                    result.nextCursor shouldBe 2L
                }
            }

            context("nationality 필터") {
                it("해당 국적의 유저만 반환한다") {
                    val krUser = AccountFixture.create(
                        id = 1L,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("한국유저"),
                        birthDate = LocalDate.of(1995, 1, 1),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    val query = DiscoverUsersQuery(
                        currentUserId = currentUserId,
                        nationality = Nation.KR,
                        size = 20,
                    )

                    every {
                        accountStore.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = Nation.KR,
                            interests = null,
                            cursor = null,
                            size = 21,
                        )
                    } returns listOf(krUser)
                    every { userInterestStore.findByAccountIds(listOf(1L)) } returns emptyList()

                    val result = useCase.execute(query)

                    result.items shouldHaveSize 1
                    result.items[0].account.nation shouldBe Nation.KR
                }
            }

            context("interests 필터") {
                it("DB에서 관심사 필터링된 유저를 반환한다") {
                    val user1 = AccountFixture.create(
                        id = 1L,
                        accountStatus = AccountStatus.ACTIVE,
                        nickname = Nickname("여행유저"),
                        birthDate = LocalDate.of(1995, 1, 1),
                        nation = Nation.KR,
                        translateLanguage = TranslateLanguage.JA,
                    )

                    val travelTag = InterestTag(id = 1, tagKey = "TRAVEL", labelKo = "여행", labelJa = "旅行", displayOrder = 1)
                    val interest1 = UserInterest(account = user1, interestTag = travelTag)

                    val query = DiscoverUsersQuery(
                        currentUserId = currentUserId,
                        interests = listOf("TRAVEL"),
                        size = 20,
                    )

                    every {
                        accountStore.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            interests = listOf("TRAVEL"),
                            cursor = null,
                            size = 21,
                        )
                    } returns listOf(user1)
                    every { userInterestStore.findByAccountIds(listOf(1L)) } returns listOf(interest1)

                    val result = useCase.execute(query)

                    result.items shouldHaveSize 1
                    result.items[0].account.id shouldBe 1L
                    result.items[0].interests shouldHaveSize 1
                    result.items[0].interests[0].interestTag.tagKey shouldBe "TRAVEL"
                }
            }

            context("결과가 비어있을 때") {
                it("빈 리스트와 hasMore=false를 반환한다") {
                    val query = DiscoverUsersQuery(currentUserId = currentUserId, size = 20)

                    every {
                        accountStore.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            interests = null,
                            cursor = null,
                            size = 21,
                        )
                    } returns emptyList()

                    val result = useCase.execute(query)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }
        }
    }
}
