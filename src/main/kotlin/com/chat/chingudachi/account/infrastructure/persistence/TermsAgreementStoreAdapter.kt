package com.chat.chingudachi.account.infrastructure.persistence

import com.chat.chingudachi.account.application.port.TermsAgreementStore
import com.chat.chingudachi.account.domain.AccountTermsAgreement
import org.springframework.stereotype.Repository

@Repository
class TermsAgreementStoreAdapter(
    private val accountTermsAgreementRepository: AccountTermsAgreementRepository,
) : TermsAgreementStore {
    override fun save(agreement: AccountTermsAgreement): AccountTermsAgreement =
        accountTermsAgreementRepository.save(agreement)

    override fun findByAccountId(accountId: Long): AccountTermsAgreement? =
        accountTermsAgreementRepository.findByAccountId(accountId)

    override fun existsByAccountId(accountId: Long): Boolean =
        accountTermsAgreementRepository.existsByAccountId(accountId)
}
