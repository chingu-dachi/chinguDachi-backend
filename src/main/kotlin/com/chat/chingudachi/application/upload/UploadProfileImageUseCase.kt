package com.chat.chingudachi.application.upload

import com.chat.chingudachi.application.upload.port.FileData
import com.chat.chingudachi.application.upload.port.FileStore
import com.chat.chingudachi.domain.common.BadRequestException
import com.chat.chingudachi.domain.common.ErrorCode
import org.springframework.stereotype.Service

@Service
class UploadProfileImageUseCase(
    private val fileStore: FileStore,
) {
    companion object {
        private const val MAX_FILE_SIZE = 5L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    fun execute(file: FileData): String {
        if (file.size > MAX_FILE_SIZE) {
            throw BadRequestException(ErrorCode.UPLOAD_FILE_TOO_LARGE)
        }

        if (file.contentType !in ALLOWED_CONTENT_TYPES) {
            throw BadRequestException(ErrorCode.UPLOAD_INVALID_FORMAT)
        }

        return fileStore.saveProfileImage(file)
    }
}
