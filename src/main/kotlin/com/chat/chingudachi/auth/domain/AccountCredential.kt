package com.chat.chingudachi.auth.domain

import com.chat.chingudachi.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "account_credential",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_credential_account_type",
            columnNames = ["account_id", "credential_type"],
        ),
        UniqueConstraint(
            name = "uk_account_credential_type_oauth_key",
            columnNames = ["credential_type", "oauth_key"],
        ),
    ],
)
class AccountCredential(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_credential_id")
    val id: Long = 0,
    @Column(name = "account_id", nullable = false)
    val accountId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 20)
    val credentialType: CredentialType,
    @Column(name = "oauth_key", length = 255)
    val oauthKey: String? = null,
) : BaseTimeEntity()

enum class CredentialType {
    GOOGLE_OAUTH,
}
