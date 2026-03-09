package com.chat.chingudachi.interest.infrastructure.persistence

import com.chat.chingudachi.interest.domain.InterestTag
import org.springframework.data.jpa.repository.JpaRepository

interface InterestTagRepository : JpaRepository<InterestTag, Long> {
    fun findAllByTagKeyIn(tagKeys: List<String>): List<InterestTag>
    fun findAllByOrderByDisplayOrderAsc(): List<InterestTag>
}
