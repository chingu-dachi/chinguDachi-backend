package com.chat.chingudachi.interest.presentation

import com.chat.chingudachi.interest.application.GetInterestTagsUseCase
import com.chat.chingudachi.interest.domain.InterestTag
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/interest-tags")
class InterestTagController(
    private val getInterestTagsUseCase: GetInterestTagsUseCase,
) {
    @GetMapping
    fun getInterestTags(): ResponseEntity<List<InterestTagResponse>> {
        val tags = getInterestTagsUseCase.execute()
        val response = tags.map { InterestTagResponse.from(it) }
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(response)
    }
}

data class InterestTagResponse(
    val id: String,
    val ko: String,
    val ja: String,
) {
    companion object {
        fun from(tag: InterestTag) = InterestTagResponse(
            id = tag.tagKey.lowercase(),
            ko = tag.labelKo,
            ja = tag.labelJa,
        )
    }
}
