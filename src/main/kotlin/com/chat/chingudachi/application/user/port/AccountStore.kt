package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.Nickname

interface AccountStore {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
    fun existsByNickname(nickname: Nickname): Boolean
}
