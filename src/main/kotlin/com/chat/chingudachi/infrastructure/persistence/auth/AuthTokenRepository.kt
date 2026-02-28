package com.chat.chingudachi.infrastructure.persistence.auth

import com.chat.chingudachi.domain.auth.AuthToken
import org.springframework.data.jpa.repository.JpaRepository

interface AuthTokenRepository : JpaRepository<AuthToken, Long>
