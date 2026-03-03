package com.chat.chingudachi.application.user.port

import com.chat.chingudachi.domain.account.AccountTermsAgreement

interface TermsAgreementStore {
    fun save(agreement: AccountTermsAgreement): AccountTermsAgreement
    fun findByAccountId(accountId: Long): AccountTermsAgreement?
    fun existsByAccountId(accountId: Long): Boolean
}
