package com.chat.chingudachi.account.controller

import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.service.AccountService
import com.chat.chingudachi.account.service.AgreeToTermsCommand
import com.chat.chingudachi.account.service.CompleteOnboardingCommand
import com.chat.chingudachi.account.service.DiscoverUsersQuery
import com.chat.chingudachi.account.service.MyProfile
import com.chat.chingudachi.account.service.UpdateProfileCommand
import com.chat.chingudachi.core.application.PaginationConstants
import com.chat.chingudachi.core.presentation.AuthAccountId
import com.chat.chingudachi.core.presentation.PaginatedResponse
import com.chat.chingudachi.core.presentation.clearRefreshTokenCookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/users")
class UserController(
    private val accountService: AccountService,
) {
    @GetMapping
    fun discoverUsers(
        @AuthAccountId accountId: Long,
        @RequestParam(required = false) nationality: Nation?,
        @RequestParam(required = false) interests: List<String>?,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ): PaginatedResponse<UserResponse> {
        val query = DiscoverUsersQuery(
            currentUserId = accountId,
            nationality = nationality,
            interests = interests,
            cursor = cursor,
            size = size.coerceIn(1, PaginationConstants.MAX_SIZE),
        )
        val result = accountService.discoverUsers(query)
        return PaginatedResponse(
            items = result.items.map { UserResponse.from(it) },
            nextCursor = result.nextCursor,
            hasMore = result.hasMore,
        )
    }

    @GetMapping("/me")
    fun getMyProfile(@AuthAccountId accountId: Long): MyProfileResponse {
        val profile = accountService.getMyProfile(accountId)
        return MyProfileResponse.from(profile)
    }

    @GetMapping("/{userId}")
    fun getUserProfile(@PathVariable userId: Long): UserResponse {
        val profile = accountService.getUserProfile(userId)
        return UserResponse.from(profile)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthAccountId accountId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): MyProfileResponse {
        val profile = accountService.updateProfile(accountId, request.toCommand())
        return MyProfileResponse.from(profile)
    }

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam nickname: String): NicknameCheckResponse {
        val available = accountService.checkNickname(nickname)
        return NicknameCheckResponse(available)
    }

    @PostMapping("/me/onboarding")
    fun completeOnboarding(
        @AuthAccountId accountId: Long,
        @Valid @RequestBody request: CompleteOnboardingRequest,
    ) {
        accountService.completeOnboarding(accountId, request.toCommand())
    }

    @PostMapping("/consent")
    fun agreeToTerms(
        @AuthAccountId accountId: Long,
        @Valid @RequestBody request: AgreeToTermsRequest,
    ): ResponseEntity<Unit> {
        accountService.agreeToTerms(accountId, request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/me")
    fun deleteAccount(
        @AuthAccountId accountId: Long,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> {
        accountService.deleteAccount(accountId)
        clearRefreshTokenCookie(response)
        return ResponseEntity.noContent().build()
    }
}

data class MyProfileResponse(
    val userId: String,
    val email: String?,
    val nickname: String?,
    val birthYear: Int?,
    val nationality: String?,
    val translateLanguage: String?,
    val city: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val onboardingRequired: Boolean,
    val consentRequired: Boolean,
    val isOnline: Boolean,
    val lastSeenAt: Instant?,
    val createdAt: Instant,
    val interests: List<String>,
) {
    companion object {
        fun from(profile: MyProfile) = MyProfileResponse(
            userId = profile.account.id.toString(),
            email = profile.account.email,
            nickname = profile.account.nickname?.value,
            birthYear = profile.account.birthDate?.year,
            nationality = profile.account.nation?.name,
            translateLanguage = profile.account.translateLanguage?.name?.lowercase(),
            city = profile.account.city,
            profileImageUrl = profile.account.profileImageUrl,
            bio = profile.account.bio,
            onboardingRequired = !profile.account.isOnboardingComplete(),
            consentRequired = profile.account.isConsentRequired(),
            isOnline = false,
            lastSeenAt = null,
            createdAt = profile.account.createdAt,
            interests = profile.interests.map { it.interestTag.tagKey },
        )
    }
}

data class NicknameCheckResponse(
    val available: Boolean,
)

data class CompleteOnboardingRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 20)
    val nickname: String,
    @field:Min(1900)
    @field:Max(2025)
    val birthYear: Int,
    @field:NotBlank
    val nationality: String,
    @field:NotEmpty
    val interests: List<String>,
    @field:Size(max = 200)
    val bio: String? = null,
    @field:Size(max = 50)
    val city: String? = null,
    val profileImageUrl: String? = null,
) {
    fun toCommand() = CompleteOnboardingCommand(
        nickname = nickname,
        birthYear = birthYear,
        nationality = nationality,
        interests = interests,
        bio = bio,
        city = city,
        profileImageUrl = profileImageUrl,
    )
}

data class UpdateProfileRequest(
    @field:Size(min = 2, max = 20)
    val nickname: String? = null,
    @field:Size(max = 200)
    val bio: String? = null,
    @field:Size(max = 50)
    val city: String? = null,
    val interests: List<String>? = null,
    val profileImageUrl: String? = null,
) {
    fun toCommand() = UpdateProfileCommand(
        nickname = nickname,
        bio = bio,
        city = city,
        interests = interests,
        profileImageUrl = profileImageUrl,
    )
}

data class AgreeToTermsRequest(
    val termsOfServiceAgree: Boolean,
    val privacyPolicyAgree: Boolean,
    val crossBorderDataAgree: Boolean,
    val marketingAgree: Boolean = false,
) {
    fun toCommand() = AgreeToTermsCommand(
        termsOfServiceAgree = termsOfServiceAgree,
        privacyPolicyAgree = privacyPolicyAgree,
        crossBorderDataAgree = crossBorderDataAgree,
        marketingAgree = marketingAgree,
    )
}
