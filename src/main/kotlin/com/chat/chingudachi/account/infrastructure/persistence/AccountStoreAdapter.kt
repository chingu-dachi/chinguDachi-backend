package com.chat.chingudachi.account.infrastructure.persistence

import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AccountStoreAdapter(
    private val accountRepository: AccountRepository,
) : AccountStore {
    override fun save(account: Account): Account = accountRepository.save(account)
    override fun findById(id: Long): Account? = accountRepository.findByIdOrNull(id)
    override fun existsByNickname(nickname: Nickname): Boolean = accountRepository.existsByNickname(nickname)

    override fun findByIds(ids: List<Long>): List<Account> {
        if (ids.isEmpty()) return emptyList()
        return accountRepository.findAllById(ids)
    }

    override fun findDiscoverableUsers(
        excludeAccountId: Long,
        nationality: Nation?,
        interests: List<String>?,
        cursor: Long?,
        size: Int,
    ): List<Account> {
        val pageable = PageRequest.of(0, size)
        return if (interests.isNullOrEmpty()) {
            accountRepository.findDiscoverableUsers(
                excludeAccountId = excludeAccountId,
                nationality = nationality,
                cursor = cursor,
                pageable = pageable,
            )
        } else {
            accountRepository.findDiscoverableUsersByInterests(
                excludeAccountId = excludeAccountId,
                nationality = nationality,
                interests = interests,
                cursor = cursor,
                pageable = pageable,
            )
        }
    }
}
