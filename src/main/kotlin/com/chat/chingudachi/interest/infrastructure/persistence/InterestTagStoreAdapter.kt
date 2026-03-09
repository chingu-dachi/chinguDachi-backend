package com.chat.chingudachi.interest.infrastructure.persistence

import com.chat.chingudachi.interest.application.port.InterestTagStore
import com.chat.chingudachi.interest.domain.InterestTag
import org.springframework.stereotype.Repository

@Repository
class InterestTagStoreAdapter(
    private val interestTagRepository: InterestTagRepository,
) : InterestTagStore {
    override fun findAllByIds(ids: List<Long>): List<InterestTag> =
        interestTagRepository.findAllById(ids)

    override fun findAllByTagKeys(tagKeys: List<String>): List<InterestTag> =
        interestTagRepository.findAllByTagKeyIn(tagKeys)

    override fun findAllOrderByDisplayOrder(): List<InterestTag> =
        interestTagRepository.findAllByOrderByDisplayOrderAsc()
}
