package com.chat.chingudachi.account.repository

import com.chat.chingudachi.account.domain.AccountTermsAgreement
import org.springframework.data.jpa.repository.JpaRepository

interface AccountTermsAgreementRepository : JpaRepository<AccountTermsAgreement, Long> {
    fun findByAccountId(accountId: Long): AccountTermsAgreement?
    fun existsByAccountId(accountId: Long): Boolean
}
