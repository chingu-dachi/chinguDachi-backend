package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.domain.account.Nickname
import org.springframework.stereotype.Service

@Service
class CheckNicknameUseCase(
    private val accountStore: AccountStore,
) {
    fun execute(nickname: String): Boolean = !accountStore.existsByNickname(Nickname(nickname))
}
