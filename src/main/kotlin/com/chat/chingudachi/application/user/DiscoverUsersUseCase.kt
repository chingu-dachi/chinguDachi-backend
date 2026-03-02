package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.application.user.query.DiscoverUsersQuery
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscoverUsersUseCase(
    private val accountStore: AccountStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional(readOnly = true)
    fun execute(query: DiscoverUsersQuery): DiscoverUsersResult {
        val accounts = accountStore.findDiscoverableUsers(
            excludeAccountId = query.currentUserId,
            nationality = query.nationality,
            interests = query.interests,
            cursor = query.cursor,
            size = query.size + 1,
        )

        val hasMore = accounts.size > query.size
        val resultAccounts = if (hasMore) accounts.dropLast(1) else accounts

        val interestsByAccountId = loadInterests(resultAccounts)
        val nextCursor = if (hasMore) resultAccounts.lastOrNull()?.id else null

        val items = resultAccounts.map { account ->
            val interests = interestsByAccountId[account.id] ?: emptyList()
            toPublicProfile(account, interests)
        }

        return DiscoverUsersResult(items, nextCursor, hasMore)
    }

    private fun loadInterests(accounts: List<Account>): Map<Long, List<UserInterest>> {
        val accountIds = accounts.map { it.id }
        if (accountIds.isEmpty()) return emptyMap()
        return userInterestStore.findByAccountIds(accountIds).groupBy { it.account.id }
    }

    private fun toPublicProfile(account: Account, interests: List<UserInterest>) =
        UserPublicProfile(
            userId = account.id,
            nickname = account.nickname?.value,
            profileImageUrl = account.profileImageUrl,
            nationality = account.nation?.name,
            translateLanguage = account.translateLanguage?.name?.lowercase(),
            birthYear = account.birthDate?.year,
            interests = interests.map { it.interestTag.tagKey },
            bio = account.bio,
            city = account.city,
            createdAt = account.createdAt,
        )
}

data class DiscoverUsersResult(
    val items: List<UserPublicProfile>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
