package com.chat.chingudachi.auth.application.port

import java.time.Duration

interface TokenProvider {
    fun createAccessToken(accountId: Long): String
    fun createRefreshToken(accountId: Long): String
    fun validateToken(token: String): Boolean
    fun parseAccountId(token: String): Long
    val refreshTokenExpiry: Duration
}
