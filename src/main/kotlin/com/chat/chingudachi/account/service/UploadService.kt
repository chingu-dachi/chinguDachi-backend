package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.infra.FileData
import com.chat.chingudachi.core.domain.BadRequestException
import com.chat.chingudachi.core.domain.ErrorCode
import org.springframework.stereotype.Service

@Service
class UploadService {
    companion object {
        private const val MAX_FILE_SIZE = 5L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val DEFAULT_PROFILE_IMAGE_URL = "DEFAULT"
    }

    fun uploadProfileImage(file: FileData): String {
        if (file.size > MAX_FILE_SIZE) {
            throw BadRequestException(ErrorCode.UPLOAD_FILE_TOO_LARGE)
        }

        if (file.contentType !in ALLOWED_CONTENT_TYPES) {
            throw BadRequestException(ErrorCode.UPLOAD_INVALID_FORMAT)
        }

        return DEFAULT_PROFILE_IMAGE_URL
    }
}
