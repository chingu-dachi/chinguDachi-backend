package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.application.user.CheckNicknameUseCase
import com.chat.chingudachi.application.user.CompleteOnboardingUseCase
import com.chat.chingudachi.application.user.DeleteAccountUseCase
import com.chat.chingudachi.application.user.GetMyProfileUseCase
import com.chat.chingudachi.application.user.GetUserProfileUseCase
import com.chat.chingudachi.application.user.MyProfile
import com.chat.chingudachi.application.user.UpdateProfileUseCase
import com.chat.chingudachi.application.user.command.CompleteOnboardingCommand
import com.chat.chingudachi.application.user.command.UpdateProfileCommand
import com.chat.chingudachi.presentation.common.AuthAccountId
import com.chat.chingudachi.presentation.common.clearRefreshTokenCookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/users")
class UserController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val checkNicknameUseCase: CheckNicknameUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) {
    @GetMapping("/me")
    fun getMyProfile(@AuthAccountId accountId: Long): MyProfileResponse {
        val profile = getMyProfileUseCase.execute(accountId)
        return MyProfileResponse.from(profile)
    }

    @GetMapping("/{userId}")
    fun getUserProfile(@PathVariable userId: Long): UserResponse {
        val profile = getUserProfileUseCase.execute(userId)
        return UserResponse.from(profile)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthAccountId accountId: Long,
        @RequestBody request: UpdateProfileRequest,
    ): MyProfileResponse {
        val profile = updateProfileUseCase.execute(accountId, request.toCommand())
        return MyProfileResponse.from(profile)
    }

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam nickname: String): NicknameCheckResponse {
        val available = checkNicknameUseCase.execute(nickname)
        return NicknameCheckResponse(available)
    }

    @PutMapping("/me")
    fun completeOnboarding(
        @AuthAccountId accountId: Long,
        @RequestBody request: CompleteOnboardingRequest,
    ) {
        completeOnboardingUseCase.execute(accountId, request.toCommand())
    }

    @DeleteMapping("/me")
    fun deleteAccount(
        @AuthAccountId accountId: Long,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> {
        deleteAccountUseCase.execute(accountId)
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
            isOnline = false, // TODO: 온라인 상태 구현 시 교체
            lastSeenAt = null, // TODO: 온라인 상태 구현 시 교체
            createdAt = profile.account.createdAt,
            interests = profile.interests.map { it.interestTag.tagKey },
        )
    }
}

data class NicknameCheckResponse(
    val available: Boolean,
)

data class CompleteOnboardingRequest(
    val nickname: String,
    val birthYear: Int,
    val nationality: String,
    val interests: List<String>,
    val bio: String? = null,
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
    val nickname: String? = null,
    val bio: String? = null,
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
