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
    @Column(length = 20, unique = true)
    var nickname: Nickname? = null,
    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var nation: Nation? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "translate_language", length = 2)
    var translateLanguage: TranslateLanguage? = null,
    @Column(length = 20)
    var city: String? = null,
    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null,
    @Column(length = 50)
    var bio: String? = null,
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
) : BaseTimeEntity() {
    fun isOnboardingComplete(): Boolean = accountStatus.isOnboarded

    fun completeOnboarding(
        nickname: Nickname,
        birthYear: Int,
        nation: Nation,
        bio: String?,
        city: String?,
        profileImageUrl: String?,
    ) {
        this.nickname = nickname
        this.birthDate = LocalDate.of(birthYear, 1, 1)
        this.nation = nation
        this.translateLanguage = nation.toTranslateLanguage()
        this.bio = bio
        this.city = city
        this.profileImageUrl = profileImageUrl
        this.accountStatus = AccountStatus.ACTIVE
    }

    fun isActive(): Boolean = accountStatus == AccountStatus.ACTIVE

    fun updateProfile(
        nickname: Nickname? = null,
        bio: String? = null,
        city: String? = null,
        profileImageUrl: String? = null,
    ) {
        nickname?.let { this.nickname = it }
        bio?.let { this.bio = it }
        city?.let { this.city = it }
        profileImageUrl?.let { this.profileImageUrl = it }
    }
}

@JvmInline
value class Nickname(
    val value: String,
) {
    init {
        require(!value.isBlank() && value.length in 2..12) { "닉네임은 2~12자여야 합니다" }
        require(!value.contains(" ")) { "닉네임에 공백을 포함할 수 없습니다" }
    }
}

enum class AccountType {
    USER,
    ADMIN,
}

enum class AccountStatus {
    NOT_CONSENT,
    NOT_ENOUGH_DETAIL,
    ACTIVE,
    INACTIVE,
    SUSPENDED, ;

    val isOnboarded: Boolean
        get() = this != NOT_CONSENT && this != NOT_ENOUGH_DETAIL
}

enum class Nation {
    KR,
    JP, ;

    fun toTranslateLanguage(): TranslateLanguage =
        when (this) {
            KR -> TranslateLanguage.JA
            JP -> TranslateLanguage.KO
        }
}

enum class TranslateLanguage {
    KO,
    JA,
}
