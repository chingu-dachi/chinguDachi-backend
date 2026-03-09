package com.chat.chingudachi.fixture

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.AccountType
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.domain.Nickname
import java.time.LocalDate

object AccountFixture {
    fun create(
        id: Long = 1L,
        accountType: AccountType = AccountType.USER,
        accountStatus: AccountStatus = AccountStatus.NOT_CONSENT,
        email: String? = "test@example.com",
        nickname: Nickname? = null,
        birthDate: LocalDate? = null,
        nation: Nation? = null,
        translateLanguage: TranslateLanguage? = null,
    ): Account =
        Account(
            id = id,
            accountType = accountType,
            accountStatus = accountStatus,
            email = email,
            nickname = nickname,
            birthDate = birthDate,
            nation = nation,
            translateLanguage = translateLanguage,
        )
}
