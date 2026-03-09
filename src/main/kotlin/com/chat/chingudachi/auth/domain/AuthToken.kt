package com.chat.chingudachi.auth.domain

import com.chat.chingudachi.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "auth_token")
class AuthToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    val id: Long = 0,
    @Column(name = "account_id", nullable = false)
    val accountId: Long,
    @Column(name = "refresh_token", nullable = false, length = 500)
    val refreshToken: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
) : BaseTimeEntity() {
    fun isExpired(): Boolean = expiresAt < Instant.now()
}
