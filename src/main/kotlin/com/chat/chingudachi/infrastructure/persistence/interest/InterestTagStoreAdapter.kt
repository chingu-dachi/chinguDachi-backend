package com.chat.chingudachi.infrastructure.persistence.interest

import com.chat.chingudachi.application.user.port.InterestTagStore
import com.chat.chingudachi.domain.interest.InterestTag
import org.springframework.stereotype.Repository

@Repository
class InterestTagStoreAdapter(
    private val interestTagRepository: InterestTagRepository,
) : InterestTagStore {
    override fun findAllByIds(ids: List<Long>): List<InterestTag> =
        interestTagRepository.findAllById(ids)
}
