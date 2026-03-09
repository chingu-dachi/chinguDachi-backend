package com.chat.chingudachi.interest.infrastructure.persistence

import com.chat.chingudachi.account.application.port.UserInterestStore
import com.chat.chingudachi.interest.domain.UserInterest
import org.springframework.stereotype.Repository

@Repository
class UserInterestStoreAdapter(
    private val userInterestRepository: UserInterestRepository,
) : UserInterestStore {
    override fun findByAccountId(accountId: Long): List<UserInterest> =
        userInterestRepository.findByAccountId(accountId)

    override fun findByAccountIds(accountIds: List<Long>): List<UserInterest> =
        userInterestRepository.findByAccountIdIn(accountIds)

    override fun saveAll(userInterests: List<UserInterest>): List<UserInterest> =
        userInterestRepository.saveAll(userInterests)

    override fun deleteAll(userInterests: List<UserInterest>) =
        userInterestRepository.deleteAll(userInterests)

    override fun deleteByAccountId(accountId: Long) =
        userInterestRepository.deleteByAccountId(accountId)
}
