package com.chat.chingudachi.domain.account

import com.chat.chingudachi.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "account")
class Account(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    var accountType: AccountType,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    var accountStatus: AccountStatus,

    @Column(length = 255)
    var email: String? = null,

    @Column(length = 20)
    var nickname: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var nation: Nation? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "native_language", length = 2)
    var nativeLanguage: NativeLanguage? = null,

    @Column(length = 20)
    var city: String? = null,

    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null,

    @Column(length = 50)
    var bio: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
) : BaseTimeEntity()
