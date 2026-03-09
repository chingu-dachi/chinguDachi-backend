package com.chat.chingudachi.auth.infrastructure.persistence

import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.CredentialType
import com.chat.chingudachi.auth.application.port.AccountCredentialStore
import org.springframework.stereotype.Repository

@Repository
class AccountCredentialStoreAdapter(
    private val accountCredentialRepository: AccountCredentialRepository,
) : AccountCredentialStore {
    override fun save(credential: AccountCredential): AccountCredential =
        accountCredentialRepository.save(credential)

    override fun findByCredentialTypeAndOauthKey(
        credentialType: CredentialType,
        oauthKey: String,
    ): AccountCredential? =
        accountCredentialRepository.findByCredentialTypeAndOauthKey(credentialType, oauthKey)
}
