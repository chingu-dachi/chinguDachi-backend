package com.chat.chingudachi.infrastructure.oauth

import com.chat.chingudachi.application.auth.port.OAuthClient
import com.chat.chingudachi.domain.auth.OAuthProvider
import com.chat.chingudachi.domain.auth.OAuthUserInfo
import com.chat.chingudachi.domain.common.ErrorCode
import com.chat.chingudachi.domain.common.InternalServerException
import com.chat.chingudachi.domain.common.UnauthorizedException
import com.chat.chingudachi.infrastructure.oauth.dto.GoogleTokenResponse
import com.chat.chingudachi.infrastructure.oauth.dto.GoogleUserInfoResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@EnableConfigurationProperties(GoogleOAuthProperties::class)
class GoogleOAuthClient(
    private val restClient: RestClient,
    private val properties: GoogleOAuthProperties,
) : OAuthClient {
    override fun authenticate(code: String): OAuthUserInfo {
        val tokenResponse = exchangeToken(code)
        val userInfo = fetchUserInfo(tokenResponse.accessToken)
        return OAuthUserInfo(
            provider = OAuthProvider.GOOGLE,
            providerUserId = userInfo.sub,
            email = userInfo.email,
        )
    }

    private fun exchangeToken(code: String): GoogleTokenResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("redirect_uri", properties.redirectUri)
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
