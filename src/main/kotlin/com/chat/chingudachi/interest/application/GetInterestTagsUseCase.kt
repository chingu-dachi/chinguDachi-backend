package com.chat.chingudachi.interest.application

import com.chat.chingudachi.interest.application.port.InterestTagStore
import com.chat.chingudachi.interest.domain.InterestTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetInterestTagsUseCase(
    private val interestTagStore: InterestTagStore,
) {
    @Transactional(readOnly = true)
    fun execute(): List<InterestTag> = interestTagStore.findAllOrderByDisplayOrder()
}
