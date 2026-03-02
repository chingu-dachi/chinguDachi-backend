package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException

interface AccountStore {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
    fun existsByNickname(nickname: Nickname): Boolean

    fun getById(id: Long): Account =
        findById(id) ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
}
