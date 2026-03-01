package com.chat.chingudachi.application.auth.port

import com.chat.chingudachi.domain.auth.OAuthUserInfo

interface OAuthClient {
    fun authenticate(code: String, redirectUri: String? = null): OAuthUserInfo
}
