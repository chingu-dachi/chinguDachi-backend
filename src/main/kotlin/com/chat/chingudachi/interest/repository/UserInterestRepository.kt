package com.chat.chingudachi.interest.repository

import com.chat.chingudachi.interest.domain.UserInterest
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface UserInterestRepository : JpaRepository<UserInterest, Long> {
    @EntityGraph(attributePaths = ["interestTag"])
    fun findByAccountId(accountId: Long): List<UserInterest>

    @EntityGraph(attributePaths = ["interestTag"])
    fun findByAccountIdIn(accountIds: List<Long>): List<UserInterest>

    fun deleteByAccountId(accountId: Long)
}
