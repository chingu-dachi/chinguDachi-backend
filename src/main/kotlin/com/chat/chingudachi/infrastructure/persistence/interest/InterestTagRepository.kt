package com.chat.chingudachi.infrastructure.persistence.interest

import com.chat.chingudachi.domain.interest.InterestTag
import org.springframework.data.jpa.repository.JpaRepository

interface InterestTagRepository : JpaRepository<InterestTag, Long>
