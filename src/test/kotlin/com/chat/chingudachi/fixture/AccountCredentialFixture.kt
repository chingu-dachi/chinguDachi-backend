package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.account.Account
import com.chat.chingudachi.domain.account.AccountCredential
import com.chat.chingudachi.domain.account.CredentialType

object AccountCredentialFixture {
    fun create(
        id: Long = 0L,
        account: Account = AccountFixture.create(id = 0L),
        credentialType: CredentialType = CredentialType.GOOGLE_OAUTH,
        oauthKey: String = "google-oauth-key-123",
    ): AccountCredential =
        AccountCredential(
            id = id,
            account = account,
            credentialType = credentialType,
            oauthKey = oauthKey,
        )
}
