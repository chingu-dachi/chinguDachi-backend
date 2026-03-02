package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException

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
