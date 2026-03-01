package com.chat.chingudachi.domain.auth

import com.chat.chingudachi.domain.account.Account
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
import java.time.Instant

@Entity
@Table(name = "auth_token")
class AuthToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,
    @Column(name = "refresh_token", nullable = false, length = 500)
    val refreshToken: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
) : BaseTimeEntity() {
    fun isExpired(): Boolean = expiresAt < Instant.now()
}
