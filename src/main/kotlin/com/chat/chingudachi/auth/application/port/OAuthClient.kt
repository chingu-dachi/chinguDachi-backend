package com.chat.chingudachi.auth.application.port

import com.chat.chingudachi.auth.domain.OAuthUserInfo

interface OAuthClient {
    fun authenticate(code: String, redirectUri: String? = null): OAuthUserInfo
}
