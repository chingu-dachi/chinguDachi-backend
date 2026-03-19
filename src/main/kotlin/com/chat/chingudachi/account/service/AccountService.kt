package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.AccountTermsAgreement
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.account.repository.AccountTermsAgreementRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.NotFoundException
import com.chat.chingudachi.interest.domain.InterestTag
import com.chat.chingudachi.interest.domain.UserInterest
import com.chat.chingudachi.interest.repository.InterestTagRepository
import com.chat.chingudachi.interest.repository.UserInterestRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val termsAgreementRepository: AccountTermsAgreementRepository,
    private val interestTagRepository: InterestTagRepository,
    private val userInterestRepository: UserInterestRepository,
    private val authTokenRepository: AuthTokenRepository,
) {
    @Transactional
    fun agreeToTerms(accountId: Long, command: AgreeToTermsCommand) {
        val account = findAccountOrThrow(accountId)

        if (!command.termsOfServiceAgree || !command.privacyPolicyAgree || !command.crossBorderDataAgree) {
            throw BadRequestException(ErrorCode.CONSENT_REQUIRED_FIELDS_MISSING)
        }

        if (termsAgreementRepository.existsByAccountId(accountId)) {
            throw ConflictException(ErrorCode.CONSENT_ALREADY_EXISTS)
        }

        val agreement = AccountTermsAgreement(
            account = account,
            isTermsOfServiceAgree = command.termsOfServiceAgree,
            isPrivacyPolicyAgree = command.privacyPolicyAgree,
            isCrossBorderDataAgree = command.crossBorderDataAgree,
            isMarketingAgree = command.marketingAgree,
        )
        termsAgreementRepository.save(agreement)

        account.agreeToTerms()
        accountRepository.save(account)

        log.info { "Terms consent completed: accountId=$accountId" }
    }

    fun checkNickname(nickname: String): Boolean = !accountRepository.existsByNickname(Nickname(nickname))

    @Transactional
    fun completeOnboarding(accountId: Long, command: CompleteOnboardingCommand) {
        val account = findAccountOrThrow(accountId)

        if (!termsAgreementRepository.existsByAccountId(accountId)) {
            throw NotFoundException(ErrorCode.CONSENT_NOT_FOUND)
        }

        if (account.isOnboardingComplete()) {
            throw ConflictException(ErrorCode.ACCOUNT_ALREADY_ONBOARDED)
        }

        val nation = Nation.entries.find { it.name == command.nationality }
            ?: throw BadRequestException(ErrorCode.INVALID_INPUT)

        if (command.birthYear < 1900 || command.birthYear > java.time.Year.now().value) {
            throw BadRequestException(ErrorCode.INVALID_INPUT)
        }

        val nickname = Nickname(command.nickname)

        if (accountRepository.existsByNickname(nickname)) {
            throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
        }

        val distinctTagKeys = command.interests.distinct()

        if (distinctTagKeys.isEmpty()) {
            throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
        }

        val interestTags = interestTagRepository.findAllByTagKeyIn(distinctTagKeys)
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
        accountRepository.save(account)

        userInterestRepository.deleteByAccountId(accountId)
        val userInterests = interestTags.map { tag -> UserInterest(account = account, interestTag = tag) }
        userInterestRepository.saveAll(userInterests)

        log.info { "Onboarding completed: accountId=$accountId, nickname=${nickname.value}, nation=$nation" }
    }

    @Transactional
    fun deleteAccount(accountId: Long) {
        val account = findAccountOrThrow(accountId)
        account.softDelete()
        accountRepository.save(account)
        userInterestRepository.deleteByAccountId(accountId)
        authTokenRepository.deleteByAccountId(accountId)

        log.info { "Account deleted: accountId=$accountId" }
    }

    @Transactional(readOnly = true)
    fun discoverUsers(query: DiscoverUsersQuery): DiscoverUsersResult {
        val accounts = findDiscoverableUsers(
            excludeAccountId = query.currentUserId,
            nationality = query.nationality,
            interests = query.interests,
            cursor = query.cursor,
            size = query.size + 1,
        )

        val hasMore = accounts.size > query.size
        val resultAccounts = if (hasMore) accounts.dropLast(1) else accounts

        val interestsByAccountId = loadInterests(resultAccounts)
        val nextCursor = if (hasMore) resultAccounts.lastOrNull()?.id else null

        val items = resultAccounts.map { account ->
            val interests = interestsByAccountId[account.id] ?: emptyList()
            toPublicProfile(account, interests)
        }

        return DiscoverUsersResult(items, nextCursor, hasMore)
    }

    @Transactional(readOnly = true)
    fun getMyProfile(accountId: Long): MyProfile {
        val account = findAccountOrThrow(accountId)
        val interests = userInterestRepository.findByAccountId(accountId)
        return MyProfile(account, interests)
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: Long): UserPublicProfile {
        val account = findAccountOrThrow(userId)

        if (!account.isActive()) {
            log.info { "Profile lookup blocked: userId=$userId, status=${account.accountStatus}" }
            throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)
        }

        val interests = userInterestRepository.findByAccountId(userId)
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

    @Transactional
    fun updateProfile(accountId: Long, command: UpdateProfileCommand): MyProfile {
        val account = findAccountOrThrow(accountId)

        val resolvedNickname = resolveNickname(command.nickname, account)
        account.updateProfile(
            nickname = resolvedNickname,
            bio = command.bio,
            city = command.city,
            profileImageUrl = command.profileImageUrl,
        )
        accountRepository.save(account)

        command.interests?.let { replaceInterests(accountId, account, it) }

        return MyProfile(account, userInterestRepository.findByAccountId(accountId))
    }

    private fun findAccountOrThrow(id: Long): Account =
        accountRepository.findByIdOrNull(id) ?: throw NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND)

    private fun findDiscoverableUsers(
        excludeAccountId: Long,
        nationality: Nation?,
        interests: List<String>?,
        cursor: Long?,
        size: Int,
    ): List<Account> {
        val pageable = PageRequest.of(0, size)
        return if (interests.isNullOrEmpty()) {
            accountRepository.findDiscoverableUsers(
                excludeAccountId = excludeAccountId,
                nationality = nationality,
                cursor = cursor,
                pageable = pageable,
            )
        } else {
            accountRepository.findDiscoverableUsersByInterests(
                excludeAccountId = excludeAccountId,
                nationality = nationality,
                interests = interests,
                cursor = cursor,
                pageable = pageable,
            )
        }
    }

    private fun loadInterests(accounts: List<Account>): Map<Long, List<UserInterest>> {
        val accountIds = accounts.map { it.id }
        if (accountIds.isEmpty()) return emptyMap()
        return userInterestRepository.findByAccountIdIn(accountIds).groupBy { it.account.id }
    }

    private fun toPublicProfile(account: Account, interests: List<UserInterest>) =
        UserPublicProfile(
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

    private fun resolveNickname(newNickname: String?, account: Account): Nickname? {
        if (newNickname == null) return null
        val nickname = Nickname(newNickname)
        if (account.nickname == nickname) return nickname
        if (accountRepository.existsByNickname(nickname)) {
            throw ConflictException(ErrorCode.ACCOUNT_NICKNAME_DUPLICATE)
        }
        return nickname
    }

    private fun replaceInterests(accountId: Long, account: Account, tagKeys: List<String>) {
        if (tagKeys.isEmpty()) throw BadRequestException(ErrorCode.INTEREST_REQUIRED)
        val distinctKeys = tagKeys.distinct()
        val tags = interestTagRepository.findAllByTagKeyIn(distinctKeys)
        if (tags.size != distinctKeys.size) {
            val missing = distinctKeys - tags.map { it.tagKey }.toSet()
            log.warn { "Interest tags not found: $missing" }
            throw BadRequestException(ErrorCode.INTEREST_TAG_NOT_FOUND)
        }

        val existing = userInterestRepository.findByAccountId(accountId)
        val (toRemove, toAdd) = calculateInterestDiff(existing, distinctKeys.toSet(), tags)

        if (toRemove.isNotEmpty()) {
            userInterestRepository.deleteAll(toRemove)
            log.info { "Removed ${toRemove.size} interests for account $accountId: ${toRemove.map { it.interestTag.tagKey }}" }
        }
        if (toAdd.isNotEmpty()) {
            userInterestRepository.saveAll(toAdd.map { UserInterest(account = account, interestTag = it) })
            log.info { "Added ${toAdd.size} interests for account $accountId: ${toAdd.map { it.tagKey }}" }
        }
    }

    private fun calculateInterestDiff(
        existing: List<UserInterest>,
        newTagKeys: Set<String>,
        newTags: List<InterestTag>,
    ): Pair<List<UserInterest>, List<InterestTag>> {
        val existingTagKeys = existing.map { it.interestTag.tagKey }.toSet()
        val toRemove = existing.filter { it.interestTag.tagKey !in newTagKeys }
        val toAdd = newTags.filter { it.tagKey !in existingTagKeys }
        return toRemove to toAdd
    }
}
