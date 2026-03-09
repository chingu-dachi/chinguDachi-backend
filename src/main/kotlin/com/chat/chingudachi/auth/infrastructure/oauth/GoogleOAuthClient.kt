package com.chat.chingudachi.auth.infrastructure.oauth

import com.chat.chingudachi.auth.application.port.OAuthClient
import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import com.chat.chingudachi.core.domain.InternalServerException
import com.chat.chingudachi.core.domain.UnauthorizedException
import com.chat.chingudachi.auth.infrastructure.oauth.dto.GoogleTokenResponse
import com.chat.chingudachi.auth.infrastructure.oauth.dto.GoogleUserInfoResponse
import org.springframework.http.HttpHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class GoogleOAuthClient(
    @Qualifier("googleRestClient") private val restClient: RestClient,
    private val properties: GoogleOAuthProperties,
) : OAuthClient {
    override fun authenticate(code: String, redirectUri: String?): OAuthUserInfo {
        val resolvedUri = redirectUri ?: properties.redirectUri
        validateRedirectUri(resolvedUri)
        val tokenResponse = exchangeToken(code, resolvedUri)
        val userInfo = fetchUserInfo(tokenResponse.accessToken)
        return OAuthUserInfo(
            provider = OAuthProvider.GOOGLE,
            providerUserId = userInfo.sub,
            email = userInfo.email,
        )
    }

    private fun validateRedirectUri(redirectUri: String) {
        val allowed = buildSet {
            add(properties.redirectUri)
            addAll(properties.allowedRedirectUris)
        }
        if (redirectUri !in allowed) {
            throw BadRequestException(ErrorCode.AUTH_OAUTH_REDIRECT_URI_NOT_ALLOWED)
        }
    }

    private fun exchangeToken(code: String, redirectUri: String): GoogleTokenResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("redirect_uri", redirectUri)
            add("grant_type", "authorization_code")
        }

        try {
            return restClient.post()
                .uri(properties.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(GoogleTokenResponse::class.java)
                ?: throw UnauthorizedException(ErrorCode.AUTH_OAUTH_CODE_INVALID)
        } catch (e: RestClientException) {
            throw UnauthorizedException(ErrorCode.AUTH_OAUTH_CODE_INVALID, e)
        }
    }

    private fun fetchUserInfo(accessToken: String): GoogleUserInfoResponse {
        try {
            return restClient.get()
                .uri(properties.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(GoogleUserInfoResponse::class.java)
                ?: throw InternalServerException(ErrorCode.AUTH_OAUTH_PROVIDER_ERROR)
        } catch (e: RestClientException) {
            throw InternalServerException(ErrorCode.AUTH_OAUTH_PROVIDER_ERROR, e)
        }
    }
}
