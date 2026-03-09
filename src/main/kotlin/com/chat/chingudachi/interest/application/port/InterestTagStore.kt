package com.chat.chingudachi.interest.application.port

import com.chat.chingudachi.interest.domain.InterestTag

interface InterestTagStore {
    fun findAllByIds(ids: List<Long>): List<InterestTag>
    fun findAllByTagKeys(tagKeys: List<String>): List<InterestTag>
    fun findAllOrderByDisplayOrder(): List<InterestTag>
}
