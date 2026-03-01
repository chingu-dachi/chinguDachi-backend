package com.chat.chingudachi.presentation.interest

import com.chat.chingudachi.application.interest.GetInterestTagsUseCase
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
    val id: Long,
    val tagKey: String,
    val labelKo: String,
    val labelJa: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(tag: com.chat.chingudachi.domain.interest.InterestTag) = InterestTagResponse(
            id = tag.id,
            tagKey = tag.tagKey,
            labelKo = tag.labelKo,
            labelJa = tag.labelJa,
            displayOrder = tag.displayOrder,
        )
    }
}
