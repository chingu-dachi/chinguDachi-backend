package com.chat.chingudachi.account.application

import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.account.domain.Nickname
import org.springframework.stereotype.Service

@Service
class CheckNicknameUseCase(
    private val accountStore: AccountStore,
) {
    fun execute(nickname: String): Boolean = !accountStore.existsByNickname(Nickname(nickname))
}
