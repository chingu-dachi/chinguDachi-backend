package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.interest.UserInterest

interface UserInterestStore {
    fun findByAccountId(accountId: Long): List<UserInterest>
    fun findByAccountIds(accountIds: List<Long>): List<UserInterest>
    fun saveAll(userInterests: List<UserInterest>): List<UserInterest>
    fun deleteByAccountId(accountId: Long)
}
