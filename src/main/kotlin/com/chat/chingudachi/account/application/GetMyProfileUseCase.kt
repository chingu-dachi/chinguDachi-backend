package com.chat.chingudachi.account.application

import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.account.application.port.UserInterestStore
import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.interest.domain.UserInterest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyProfileUseCase(
    private val accountStore: AccountStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional(readOnly = true)
    fun execute(accountId: Long): MyProfile {
        val account = accountStore.getById(accountId)
        val interests = userInterestStore.findByAccountId(accountId)
        return MyProfile(account, interests)
    }
}

data class MyProfile(
    val account: Account,
    val interests: List<UserInterest>,
)
