package com.chat.chingudachi.account.infrastructure.persistence

import com.chat.chingudachi.account.domain.AccountCredential
import com.chat.chingudachi.account.domain.CredentialType
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCredentialRepository : JpaRepository<AccountCredential, Long> {
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}
