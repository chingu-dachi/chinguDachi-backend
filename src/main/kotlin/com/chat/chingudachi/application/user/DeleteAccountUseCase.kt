package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AuthTokenStore
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

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
        userInterestStore.deleteByAccountId(accountId)
        authTokenStore.deleteByAccountId(accountId)

        log.info { "Account deleted: accountId=$accountId" }
    }
}
