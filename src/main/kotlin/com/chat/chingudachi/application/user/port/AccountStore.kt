package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname

interface AccountStore {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
    fun existsByNickname(nickname: Nickname): Boolean
    fun findDiscoverableUsers(
        excludeAccountId: Long,
        nationality: Nation? = null,
        cursor: Long? = null,
        size: Int = 21,
    ): List<Account>
}
