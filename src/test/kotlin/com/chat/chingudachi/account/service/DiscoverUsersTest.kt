package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.interest.domain.InterestTag
import com.chat.chingudachi.interest.domain.UserInterest
import com.chat.chingudachi.interest.repository.InterestTagRepository
import com.chat.chingudachi.interest.repository.UserInterestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

class DiscoverUsersTest : DescribeSpec() {
    private val accountRepository = mockk<AccountRepository>()
    private val termsAgreementRepository = mockk<AccountTermsAgreementRepository>()
    private val interestTagRepository = mockk<InterestTagRepository>()
    private val userInterestRepository = mockk<UserInterestRepository>()
    private val authTokenRepository = mockk<AuthTokenRepository>()
    private val accountService = AccountService(
        accountRepository, termsAgreementRepository, interestTagRepository, userInterestRepository, authTokenRepository,
    )

    init {
        afterEach { clearMocks(accountRepository, userInterestRepository) }

        describe("discoverUsers") {
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
                        accountRepository.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            cursor = null,
                            pageable = PageRequest.of(0, 21),
                        )
                    } returns listOf(user1, user2)
                    every { userInterestRepository.findByAccountIdIn(listOf(1L, 2L)) } returns emptyList()

                    val result = accountService.discoverUsers(query)

                    result.items shouldHaveSize 2
                    result.items[0].userId shouldBe 1L
                    result.items[1].userId shouldBe 2L
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
                        accountRepository.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            cursor = null,
                            pageable = PageRequest.of(0, 3),
                        )
                    } returns users
                    every { userInterestRepository.findByAccountIdIn(listOf(1L, 2L)) } returns emptyList()

                    val result = accountService.discoverUsers(query)

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
                        accountRepository.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = Nation.KR,
                            cursor = null,
                            pageable = PageRequest.of(0, 21),
                        )
                    } returns listOf(krUser)
                    every { userInterestRepository.findByAccountIdIn(listOf(1L)) } returns emptyList()

                    val result = accountService.discoverUsers(query)

                    result.items shouldHaveSize 1
                    result.items[0].nationality shouldBe "KR"
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
                        accountRepository.findDiscoverableUsersByInterests(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            interests = listOf("TRAVEL"),
                            cursor = null,
                            pageable = PageRequest.of(0, 21),
                        )
                    } returns listOf(user1)
                    every { userInterestRepository.findByAccountIdIn(listOf(1L)) } returns listOf(interest1)

                    val result = accountService.discoverUsers(query)

                    result.items shouldHaveSize 1
                    result.items[0].userId shouldBe 1L
                    result.items[0].interests shouldBe listOf("TRAVEL")
                }
            }

            context("결과가 비어있을 때") {
                it("빈 리스트와 hasMore=false를 반환한다") {
                    val query = DiscoverUsersQuery(currentUserId = currentUserId, size = 20)

                    every {
                        accountRepository.findDiscoverableUsers(
                            excludeAccountId = currentUserId,
                            nationality = null,
                            cursor = null,
                            pageable = PageRequest.of(0, 21),
                        )
                    } returns emptyList()

                    val result = accountService.discoverUsers(query)

                    result.items.shouldBeEmpty()
                    result.hasMore shouldBe false
                    result.nextCursor shouldBe null
                }
            }
        }
    }
}
