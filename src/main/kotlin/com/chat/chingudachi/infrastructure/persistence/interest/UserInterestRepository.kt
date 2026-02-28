package com.chat.chingudachi.infrastructure.persistence.interest

import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.data.jpa.repository.JpaRepository

interface UserInterestRepository : JpaRepository<UserInterest, Long>
