package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.auth.port.AccountStore
import com.chat.chingudachi.application.user.port.InterestTagStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ConflictException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class CompleteOnboardingCommand(
    val nickname: String,
    val birthDate: LocalDate,
    val nation: Nation,
    val interestTagIds: List<Long>,
)

@Service
class CompleteOnboardingUseCase(
    private val accountStore: AccountStore,
    private val interestTagStore: InterestTagStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional
    fun execute(accountId: Long, command: CompleteOnboardingCommand) {
        val account = accountStore.findById(accountId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

        val nickname = Nickname(command.nickname)

        if (accountStore.existsByNickname(nickname)) {
            throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
        }

        if (command.interestTagIds.isEmpty()) {
            throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
        }

        val interestTags = interestTagStore.findAllByIds(command.interestTagIds)
        if (interestTags.size != command.interestTagIds.size) {
            throw BadRequestException(ErrorCode.INTEREST_TAG_NOT_FOUND)
        }

        account.completeOnboarding(nickname, command.birthDate, command.nation)
        accountStore.save(account)

        userInterestStore.deleteByAccountId(accountId)
        val userInterests = interestTags.map { tag -> UserInterest(account = account, interestTag = tag) }
        userInterestStore.saveAll(userInterests)
    }
}
