package com.chat.chingudachi.application.auth

import java.time.Duration

object AuthConstants {
    val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(30)
}
