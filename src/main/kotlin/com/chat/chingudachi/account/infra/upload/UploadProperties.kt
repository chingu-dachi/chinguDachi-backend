package com.chat.chingudachi.account.infra.upload

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("upload.profile-image")
data class UploadProperties(
    val directory: String,
    val baseUrl: String,
)
