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
            cursor = query.cursor,
            size = query.size + 1,
        )

        val hasMore = accounts.size > query.size
        val resultAccounts = if (hasMore) accounts.dropLast(1) else accounts

        val accountIds = resultAccounts.map { it.id }
        val interestsByAccountId = if (accountIds.isNotEmpty()) {
            userInterestStore.findByAccountIds(accountIds).groupBy { it.account.id }
        } else {
            emptyMap()
        }

        val filteredAccounts = if (query.interests.isNullOrEmpty()) {
            resultAccounts
        } else {
            resultAccounts.filter { account ->
                val accountInterests = interestsByAccountId[account.id] ?: emptyList()
                accountInterests.any { it.interestTag.tagKey in query.interests }
            }
        }

        val nextCursor = if (hasMore) resultAccounts.lastOrNull()?.id else null

        val items = filteredAccounts.map { account ->
            val interests = interestsByAccountId[account.id] ?: emptyList()
            DiscoverUserItem(account, interests)
        }

        return DiscoverUsersResult(items, nextCursor, hasMore)
    }
}

data class DiscoverUserItem(
    val account: Account,
    val interests: List<UserInterest>,
)

data class DiscoverUsersResult(
    val items: List<DiscoverUserItem>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
