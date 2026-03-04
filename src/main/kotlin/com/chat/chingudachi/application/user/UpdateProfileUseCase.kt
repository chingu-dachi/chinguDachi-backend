package com.chat.chingudachi.application.user

import com.chat.chingudachi.application.interest.port.InterestTagStore
import com.chat.chingudachi.application.user.command.UpdateProfileCommand
import com.chat.chingudachi.application.user.port.AccountStore
import com.chat.chingudachi.application.user.port.UserInterestStore
import com.chat.chingudachi.domain.account.Account
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
class UpdateProfileUseCase(
    private val accountStore: AccountStore,
    private val interestTagStore: InterestTagStore,
    private val userInterestStore: UserInterestStore,
) {
    @Transactional
    fun execute(accountId: Long, command: UpdateProfileCommand): MyProfile {
        val account = accountStore.getById(accountId)

        val resolvedNickname = resolveNickname(command.nickname, account)
        account.updateProfile(
            nickname = resolvedNickname,
            bio = command.bio,
            city = command.city,
            profileImageUrl = command.profileImageUrl,
        )
        accountStore.save(account)

        command.interests?.let { replaceInterests(accountId, account, it) }

        return MyProfile(account, userInterestStore.findByAccountId(accountId))
    }

    private fun resolveNickname(newNickname: String?, account: Account): Nickname? {
        if (newNickname == null) return null
        val nickname = Nickname(newNickname)
        if (account.nickname == nickname) return nickname
        if (accountStore.existsByNickname(nickname)) {
            throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
        }
        return nickname
    }

    private fun replaceInterests(accountId: Long, account: Account, tagKeys: List<String>) {
        if (tagKeys.isEmpty()) throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
        val distinctKeys = tagKeys.distinct()
        val tags = interestTagStore.findAllByTagKeys(distinctKeys)
        if (tags.size != distinctKeys.size) {
            val missing = distinctKeys - tags.map { it.tagKey }.toSet()
            log.warn { "Interest tags not found: $missing" }
            throw BadRequestException(ErrorCode.INTEREST_TAG_NOT_FOUND)
        }

        val existing = userInterestStore.findByAccountId(accountId)
        val existingTagKeys = existing.map { it.interestTag.tagKey }.toSet()
        val newTagKeys = distinctKeys.toSet()

        val toRemove = existing.filter { it.interestTag.tagKey !in newTagKeys }
        val toAdd = tags.filter { it.tagKey !in existingTagKeys }

        if (toRemove.isNotEmpty()) userInterestStore.deleteAll(toRemove)
        if (toAdd.isNotEmpty()) userInterestStore.saveAll(toAdd.map { UserInterest(account = account, interestTag = it) })
    }
}
