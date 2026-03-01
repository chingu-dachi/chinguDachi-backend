package com.chat.chingudachi.fixture

import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo

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
