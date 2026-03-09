package com.chat.chingudachi.account.application.port

import com.chat.chingudachi.account.domain.AccountTermsAgreement

interface TermsAgreementStore {
    fun save(agreement: AccountTermsAgreement): AccountTermsAgreement
    fun findByAccountId(accountId: Long): AccountTermsAgreement?
    fun existsByAccountId(accountId: Long): Boolean
}
