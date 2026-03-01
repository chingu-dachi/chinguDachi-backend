package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.auth.port.UserInterestStore
import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyProfileUseCase(
    private val accountStore: AccountStore,
    private val userInterestStore: UserInterestStore,
) {
    fun execute(accountId: Long): MyProfile {
        val account = accountStore.findById(accountId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        val interests = userInterestStore.findByAccountId(accountId)
        return MyProfile(account, interests)
    }
}

data class MyProfile(
    val account: Account,
    val interests: List<UserInterest>,
)
