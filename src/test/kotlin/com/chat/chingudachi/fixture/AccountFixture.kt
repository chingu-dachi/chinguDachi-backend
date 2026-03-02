package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountStatus
import com.chat.chingudachi.domain.account.AccountType
import com.chat.chingudachi.domain.account.Nation
import com.chat.chingudachi.domain.account.TranslateLanguage
import com.chat.chingudachi.domain.account.Nickname
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
