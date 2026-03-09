package com.chat.chingudachi.account.application.port

import com.chat.chingudachi.interest.domain.UserInterest

interface UserInterestStore {
    fun findByAccountId(accountId: Long): List<UserInterest>
    fun findByAccountIds(accountIds: List<Long>): List<UserInterest>
    fun saveAll(userInterests: List<UserInterest>): List<UserInterest>
    fun deleteAll(userInterests: List<UserInterest>)
    fun deleteByAccountId(accountId: Long)
}
