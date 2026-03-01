package com.chat.chingudachi.infrastructure.jwt

import com.chat.chingudachi.application.auth.port.TokenProvider
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.UnauthorizedException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) : TokenProvider {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        Decoders.BASE64.decode(jwtProperties.secret),
    )

    override fun createAccessToken(accountId: Long): String =
        createToken(accountId, jwtProperties.accessTokenExpiry.toMillis())

    override fun createRefreshToken(accountId: Long): String =
        createToken(accountId, jwtProperties.refreshTokenExpiry.toMillis())

    fun validateToken(token: String): Boolean =
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    fun parseAccountId(token: String): Long =
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
                .toLong()
        } catch (e: ExpiredJwtException) {
            throw UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED, e)
        } catch (e: JwtException) {
            throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID, e)
        } catch (e: IllegalArgumentException) {
            throw UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID, e)
        }

    private fun createToken(accountId: Long, expiryMillis: Long): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(accountId.toString())
            .issuedAt(Date(now))
            .expiration(Date(now + expiryMillis))
            .signWith(secretKey)
            .compact()
    }
}
