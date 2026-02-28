package com.chat.chingudachi.domain.interest

import com.chat.chingudachi.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "interest_tag")
class InterestTag(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_tag_id")
    val id: Long = 0,

    @Column(name = "tag_key", nullable = false, unique = true, length = 20)
    val tagKey: String,

    @Column(name = "label_ko", nullable = false, length = 20)
    val labelKo: String,

    @Column(name = "label_ja", nullable = false, length = 20)
    val labelJa: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
) : BaseTimeEntity()
