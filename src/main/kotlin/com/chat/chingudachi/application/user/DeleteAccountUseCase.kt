package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteAccountUseCase(
    private val accountStore: AccountStore,
    private val authTokenStore: AuthTokenStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional
    fun execute(accountId: Long) {
        val account = accountStore.findById(accountId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        account.softDelete()
        accountStore.save(account)
        authTokenStore.deleteByAccountId(accountId)
        userInterestStore.deleteByAccountId(accountId)
    }
}
