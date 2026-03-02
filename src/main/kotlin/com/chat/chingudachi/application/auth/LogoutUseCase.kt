package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.port.AuthTokenStore
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
