package com.chat.chingudachi.fixture

import com.chat.chingudachi.auth.domain.AccountCredential
import com.chat.chingudachi.auth.domain.CredentialType

object AccountCredentialFixture {
    fun create(
        id: Long = 0L,
        accountId: Long = 0L,
        credentialType: CredentialType = CredentialType.GOOGLE_OAUTH,
        oauthKey: String = "google-oauth-key-123",
    ): AccountCredential =
        AccountCredential(
            id = id,
            accountId = accountId,
            credentialType = credentialType,
            oauthKey = oauthKey,
        )
}
