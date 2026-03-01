package com.chat.chingudachi.domain.interest

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
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_interest",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_interest_account_tag",
            columnNames = ["account_id", "interest_tag_id"],
        ),
    ],
)
class UserInterest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_interest_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_tag_id", nullable = false)
    val interestTag: InterestTag,
) : BaseTimeEntity()
