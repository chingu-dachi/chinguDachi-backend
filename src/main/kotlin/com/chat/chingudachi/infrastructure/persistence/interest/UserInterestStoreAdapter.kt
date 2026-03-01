package com.chat.chingudachi.infrastructure.persistence.interest

import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.stereotype.Repository

@Repository
class UserInterestStoreAdapter(
    private val userInterestRepository: UserInterestRepository,
) : UserInterestStore {
    override fun findByAccountId(accountId: Long): List<UserInterest> =
        userInterestRepository.findByAccountId(accountId)
}
