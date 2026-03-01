package com.chat.chingudachi.application.interest

import com.chat.chingudachi.application.user.port.InterestTagStore
import com.chat.chingudachi.domain.interest.InterestTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetInterestTagsUseCase(
    private val interestTagStore: InterestTagStore,
) {
    @Transactional(readOnly = true)
    fun execute(): List<InterestTag> = interestTagStore.findAllOrderByDisplayOrder()
}
