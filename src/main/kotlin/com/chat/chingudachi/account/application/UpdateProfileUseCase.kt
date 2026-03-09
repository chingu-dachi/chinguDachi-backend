package com.chat.chingudachi.account.application

import com.chat.chingudachi.interest.application.port.InterestTagStore
import com.chat.chingudachi.account.application.command.UpdateProfileCommand
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.interest.application.port.UserInterestStore
import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.interest.domain.UserInterest
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
        val (toRemove, toAdd) = calculateInterestDiff(existing, distinctKeys.toSet(), tags)

        if (toRemove.isNotEmpty()) {
            userInterestStore.deleteAll(toRemove)
            log.info { "Removed ${toRemove.size} interests for account $accountId: ${toRemove.map { it.interestTag.tagKey }}" }
        }
        if (toAdd.isNotEmpty()) {
            userInterestStore.saveAll(toAdd.map { UserInterest(account = account, interestTag = it) })
            log.info { "Added ${toAdd.size} interests for account $accountId: ${toAdd.map { it.tagKey }}" }
        }
    }

    private fun calculateInterestDiff(
        existing: List<UserInterest>,
        newTagKeys: Set<String>,
        newTags: List<com.chat.chingudachi.interest.domain.InterestTag>,
    ): Pair<List<UserInterest>, List<com.chat.chingudachi.interest.domain.InterestTag>> {
        val existingTagKeys = existing.map { it.interestTag.tagKey }.toSet()
        val toRemove = existing.filter { it.interestTag.tagKey !in newTagKeys }
        val toAdd = newTags.filter { it.tagKey !in existingTagKeys }
        return toRemove to toAdd
    }
}
