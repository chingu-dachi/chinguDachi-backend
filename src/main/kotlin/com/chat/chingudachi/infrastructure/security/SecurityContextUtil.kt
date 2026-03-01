package com.chat.chingudachi.infrastructure.security

import com.chat.chingudachi.domain.common.UnauthorizedException
import org.springframework.security.core.context.SecurityContextHolder

object SecurityContextUtil {
    fun getCurrentAccountId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException()
        return authentication.principal as? Long
            ?: throw UnauthorizedException()
    }
}
