package com.chat.chingudachi.auth.application

import com.chat.chingudachi.auth.application.port.AuthTokenStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class LogoutUseCase(
    private val authTokenStore: AuthTokenStore,
) {
    @Transactional
    fun execute(accountId: Long) {
        authTokenStore.deleteByAccountId(accountId)
        log.debug { "Logout: accountId=$accountId" }
    }
}
