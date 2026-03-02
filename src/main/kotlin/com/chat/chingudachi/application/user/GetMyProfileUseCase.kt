package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.interest.UserInterest
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
