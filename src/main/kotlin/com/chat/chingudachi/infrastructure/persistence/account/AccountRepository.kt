package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.Nickname
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
    fun existsByNickname(nickname: Nickname): Boolean
}
