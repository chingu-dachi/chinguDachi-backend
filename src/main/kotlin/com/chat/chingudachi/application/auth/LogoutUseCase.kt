package com.chat.chingudachi.application.auth

import com.chat.chingudachi.application.auth.port.AuthTokenStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogoutUseCase(
    private val authTokenStore: AuthTokenStore,
) {
    @Transactional
    fun execute(accountId: Long) {
        authTokenStore.deleteByAccountId(accountId)
    }
}
