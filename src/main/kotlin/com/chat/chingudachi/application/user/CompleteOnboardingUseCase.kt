package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.interest.port.InterestTagStore
import com.chat.chingudachi.application.user.command.CompleteOnboardingCommand
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ConflictException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.interest.UserInterest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class CompleteOnboardingUseCase(
    private val accountStore: AccountStore,
    private val interestTagStore: InterestTagStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional
    fun execute(accountId: Long, command: CompleteOnboardingCommand) {
        val account = accountStore.getById(accountId)

        if (account.isOnboardingComplete()) {
            throw ConflictException(ErrorCode.ACCOUNT_ALREADY_ONBOARDED)
        }

        val nation = Nation.entries.find { it.name == command.nationality }
            ?: throw BadRequestException(ErrorCode.INVALID_INPUT)

        if (command.birthYear < 1900 || command.birthYear > java.time.Year.now().value) {
            throw BadRequestException(ErrorCode.INVALID_INPUT)
        }

        val nickname = Nickname(command.nickname)

        if (accountStore.existsByNickname(nickname)) {
            throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
        }

        val distinctTagKeys = command.interests.distinct()

        if (distinctTagKeys.isEmpty()) {
            throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
        }

        val interestTags = interestTagStore.findAllByTagKeys(distinctTagKeys)
        if (interestTags.size != distinctTagKeys.size) {
            throw BadRequestException(ErrorCode.INTEREST_TAG_NOT_FOUND)
        }

        account.completeOnboarding(
            nickname = nickname,
            birthYear = command.birthYear,
            nation = nation,
            bio = command.bio,
            city = command.city,
            profileImageUrl = command.profileImageUrl,
        )
        accountStore.save(account)

        userInterestStore.deleteByAccountId(accountId)
        val userInterests = interestTags.map { tag -> UserInterest(account = account, interestTag = tag) }
        userInterestStore.saveAll(userInterests)

        log.info { "Onboarding completed: accountId=$accountId, nickname=${nickname.value}, nation=$nation" }
    }
}
