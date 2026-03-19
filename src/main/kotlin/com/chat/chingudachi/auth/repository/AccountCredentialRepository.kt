package com.chat.chingudachi.auth.repository

import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.CredentialType
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCredentialRepository : JpaRepository<AccountCredential, Long> {
    fun findByCredentialTypeAndOauthKey(credentialType: CredentialType, oauthKey: String): AccountCredential?
}
