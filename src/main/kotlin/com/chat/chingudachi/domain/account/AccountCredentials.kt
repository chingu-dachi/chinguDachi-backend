package com.chat.chingudachi.domain.account

import com.chat.chingudachi.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account_credentials")
class AccountCredentials(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_credentials_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Column(name = "credential_type", nullable = false, length = 20)
    val credentialType: String,

    @Column(name = "oauth_key", length = 255)
    val oauthKey: String? = null,
) : BaseTimeEntity()
