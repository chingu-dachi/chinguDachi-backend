package com.chat.chingudachi.account.application

import com.chat.chingudachi.account.application.command.AgreeToTermsCommand
import com.chat.chingudachi.account.application.port.AccountStore
import com.chat.chingudachi.account.application.port.TermsAgreementStore
import com.chat.chingudachi.account.domain.AccountTermsAgreement
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ConflictException
import com.chat.chingudachi.core.domain.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class AgreeToTermsUseCase(
    private val accountStore: AccountStore,
    private val termsAgreementStore: TermsAgreementStore,
) {
    @Transactional
    fun execute(accountId: Long, command: AgreeToTermsCommand) {
        val account = accountStore.getById(accountId)

        if (!command.termsOfServiceAgree || !command.privacyPolicyAgree || !command.crossBorderDataAgree) {
            throw BadRequestException(ErrorCode.CONSENT_REQUIRED_FIELDS_MISSING)
        }

        if (termsAgreementStore.existsByAccountId(accountId)) {
            throw ConflictException(ErrorCode.CONSENT_ALREADY_EXISTS)
        }

        val agreement = AccountTermsAgreement(
            account = account,
            isTermsOfServiceAgree = command.termsOfServiceAgree,
            isPrivacyPolicyAgree = command.privacyPolicyAgree,
            isCrossBorderDataAgree = command.crossBorderDataAgree,
            isMarketingAgree = command.marketingAgree,
        )
        termsAgreementStore.save(agreement)

        account.agreeToTerms()
        accountStore.save(account)

        log.info { "Terms consent completed: accountId=$accountId" }
    }
}
