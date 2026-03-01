package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.CredentialType
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCredentialRepository : JpaRepository<AccountCredential, Long> {
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}
