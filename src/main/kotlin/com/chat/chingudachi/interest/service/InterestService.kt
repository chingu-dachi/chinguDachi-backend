package com.chat.chingudachi.interest.service

import com.chat.chingudachi.interest.domain.InterestTag
import com.chat.chingudachi.interest.repository.InterestTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InterestService(
    private val interestTagRepository: InterestTagRepository,
) {
    @Transactional(readOnly = true)
    fun getInterestTags(): List<InterestTag> = interestTagRepository.findAllByOrderByDisplayOrderAsc()
}
