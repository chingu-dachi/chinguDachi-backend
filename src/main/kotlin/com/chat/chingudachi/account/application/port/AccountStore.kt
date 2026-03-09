package com.chat.chingudachi.account.application.port

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException

interface AccountStore {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
    fun existsByNickname(nickname: Nickname): Boolean
    fun findDiscoverableUsers(
        excludeAccountId: Long,
        nationality: Nation? = null,
        interests: List<String>? = null,
        cursor: Long? = null,
        size: Int,
    ): List<Account>

    fun findByIds(ids: List<Long>): List<Account>

    fun getById(id: Long): Account =
        findById(id) ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
}
