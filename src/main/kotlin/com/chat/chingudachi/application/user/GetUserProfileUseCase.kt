package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserProfileUseCase(
    private val accountStore: AccountStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): MyProfile {
        val account = accountStore.findById(userId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

        if (!account.isOnboardingComplete() || account.deletedAt != null) {
            throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        }

        val interests = userInterestStore.findByAccountId(userId)
        return MyProfile(account, interests)
    }
}
