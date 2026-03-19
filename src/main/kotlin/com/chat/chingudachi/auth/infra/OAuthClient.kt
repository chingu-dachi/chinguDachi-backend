package com.chat.chingudachi.auth.infra

import com.chat.chingudachi.auth.domain.OAuthUserInfo

interface OAuthClient {
    fun authenticate(code: String, redirectUri: String?): OAuthUserInfo
}
