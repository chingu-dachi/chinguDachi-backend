package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class GetUserProfileUseCase(
    private val accountStore: AccountStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): UserPublicProfile {
        val account = accountStore.getById(userId)

        if (!account.isActive()) {
            log.info { "Profile lookup blocked: userId=$userId, status=${account.accountStatus}" }
            throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        }

        val interests = userInterestStore.findByAccountId(userId)
        return UserPublicProfile(
            userId = account.id,
            nickname = account.nickname?.value,
            profileImageUrl = account.profileImageUrl,
            nationality = account.nation?.name,
            translateLanguage = account.translateLanguage?.name?.lowercase(),
            birthYear = account.birthDate?.year,
            interests = interests.map { it.interestTag.tagKey },
            bio = account.bio,
            city = account.city,
            createdAt = account.createdAt,
        )
    }
}
