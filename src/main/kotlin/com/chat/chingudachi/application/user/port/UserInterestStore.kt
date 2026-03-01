package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.interest.UserInterest

interface UserInterestStore {
    fun findByAccountId(accountId: Long): List<UserInterest>
}
