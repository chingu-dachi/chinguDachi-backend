package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.Account
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long>
