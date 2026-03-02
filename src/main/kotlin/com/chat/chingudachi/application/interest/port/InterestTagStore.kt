package com.chat.chingudachi.application.interest.port

import com.chat.chingudachi.domain.interest.InterestTag

interface InterestTagStore {
    fun findAllByIds(ids: List<Long>): List<InterestTag>
    fun findAllByTagKeys(tagKeys: List<String>): List<InterestTag>
    fun findAllOrderByDisplayOrder(): List<InterestTag>
}
