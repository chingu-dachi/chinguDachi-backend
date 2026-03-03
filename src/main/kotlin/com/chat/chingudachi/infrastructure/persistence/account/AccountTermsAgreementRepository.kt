package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.AccountTermsAgreement
import org.springframework.data.jpa.repository.JpaRepository

interface AccountTermsAgreementRepository : JpaRepository<AccountTermsAgreement, Long> {
    fun findByAccountId(accountId: Long): AccountTermsAgreement?
    fun existsByAccountId(accountId: Long): Boolean
}
