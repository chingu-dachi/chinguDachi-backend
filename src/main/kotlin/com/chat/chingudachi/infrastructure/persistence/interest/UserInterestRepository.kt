package com.chat.chingudachi.infrastructure.persistence.interest

import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface UserInterestRepository : JpaRepository<UserInterest, Long> {
    @EntityGraph(attributePaths = ["interestTag"])
    fun findByAccountId(accountId: Long): List<UserInterest>
    fun deleteByAccountId(accountId: Long)
}
