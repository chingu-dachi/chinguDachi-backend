package com.chat.chingudachi.presentation.user

import com.chat.chingudachi.application.user.CheckNicknameUseCase
import com.chat.chingudachi.application.user.GetMyProfileUseCase
import com.chat.chingudachi.application.user.MyProfile
import com.chat.chingudachi.presentation.common.AuthAccountId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/users")
class UserController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val checkNicknameUseCase: CheckNicknameUseCase,
) {
    @GetMapping("/me")
    fun getMyProfile(@AuthAccountId accountId: Long): MyProfileResponse {
        val profile = getMyProfileUseCase.execute(accountId)
        return MyProfileResponse.from(profile)
    }

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam nickname: String): NicknameCheckResponse {
        val available = checkNicknameUseCase.execute(nickname)
        return NicknameCheckResponse(available)
    }
}

data class MyProfileResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val birthDate: LocalDate?,
    val nation: String?,
    val nativeLanguage: String?,
    val city: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val accountStatus: String,
    val interests: List<InterestResponse>,
) {
    companion object {
        fun from(profile: MyProfile) = MyProfileResponse(
            id = profile.account.id,
            email = profile.account.email,
            nickname = profile.account.nickname?.value,
            birthDate = profile.account.birthDate,
            nation = profile.account.nation?.name,
            nativeLanguage = profile.account.nativeLanguage?.name,
            city = profile.account.city,
            profileImageUrl = profile.account.profileImageUrl,
            bio = profile.account.bio,
            accountStatus = profile.account.accountStatus.name,
            interests = profile.interests.map { ui ->
                InterestResponse(
                    tagKey = ui.interestTag.tagKey,
                    labelKo = ui.interestTag.labelKo,
                    labelJa = ui.interestTag.labelJa,
                )
            },
        )
    }
}

data class InterestResponse(
    val tagKey: String,
    val labelKo: String,
    val labelJa: String,
)

data class NicknameCheckResponse(
    val available: Boolean,
)
