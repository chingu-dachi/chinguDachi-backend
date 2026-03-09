package com.chat.chingudachi.fixture

import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo

object OAuthUserInfoFixture {
    fun create(
        provider: OAuthProvider = OAuthProvider.GOOGLE,
        providerUserId: String = "google-user-id-123",
        email: String = "test@gmail.com",
    ): OAuthUserInfo =
        OAuthUserInfo(
            provider = provider,
            providerUserId = providerUserId,
            email = email,
        )
}
