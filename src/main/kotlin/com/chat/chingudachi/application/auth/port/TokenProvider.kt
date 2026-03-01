package com.chat.chingudachi.application.auth.port

interface TokenProvider {
    fun createAccessToken(accountId: Long): String
    fun createRefreshToken(accountId: Long): String
}
