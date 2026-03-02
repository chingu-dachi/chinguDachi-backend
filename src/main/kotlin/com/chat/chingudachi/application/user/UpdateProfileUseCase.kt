package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.interest.port.InterestTagStore
import com.chat.chingudachi.application.user.command.UpdateProfileCommand
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.Nickname
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ConflictException
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.NotFoundException
import com.chat.chingudachi.domain.interest.UserInterest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateProfileUseCase(
    private val accountStore: AccountStore,
    private val interestTagStore: InterestTagStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional
    fun execute(accountId: Long, command: UpdateProfileCommand): MyProfile {
        val account = accountStore.findById(accountId)
            ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

        command.nickname?.let { newNickname ->
            val nickname = Nickname(newNickname)
            if (account.nickname != nickname && accountStore.existsByNickname(nickname)) {
                throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
            }
            account.nickname = nickname
        }

        command.bio?.let { account.bio = it }
        command.city?.let { account.city = it }
        command.profileImageUrl?.let { account.profileImageUrl = it }

        accountStore.save(account)

        command.interests?.let { tagKeys ->
            if (tagKeys.isEmpty()) {
                throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
            }
            val distinctKeys = tagKeys.distinct()
            val tags = interestTagStore.findAllByTagKeys(distinctKeys)
            if (tags.size != distinctKeys.size) {
                throw BadRequestException(ErrorCode.INTEREST_TAG_NOT_FOUND)
            }
            userInterestStore.deleteByAccountId(accountId)
            val userInterests = tags.map { tag -> UserInterest(account = account, interestTag = tag) }
            userInterestStore.saveAll(userInterests)
        }

        val interests = userInterestStore.findByAccountId(accountId)
        return MyProfile(account, interests)
    }
}
