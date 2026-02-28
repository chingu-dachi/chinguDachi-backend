package com.chat.chingudachi.domain.account

import com.chat.chingudachi.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account_terms_agreement")
class AccountTermsAgreement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_terms_agreement_id")
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Column(name = "is_terms_of_service_agree", nullable = false)
    val isTermsOfServiceAgree: Boolean,

    @Column(name = "is_privacy_policy_agree", nullable = false)
    val isPrivacyPolicyAgree: Boolean,

    @Column(name = "is_marketing_agree", nullable = false)
    val isMarketingAgree: Boolean,
) : BaseTimeEntity()
