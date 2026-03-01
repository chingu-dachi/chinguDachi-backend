package com.chat.chingudachi.application.user.command

import com.chat.chingudachi.domain.account.Nation
import java.time.LocalDate

data class CompleteOnboardingCommand(
    val nickname: String,
    val birthDate: LocalDate,
    val nation: Nation,
    val interestTagIds: List<Long>,
)
