package com.chat.chingudachi.application.upload.port

import org.springframework.web.multipart.MultipartFile

interface FileStore {
    fun saveProfileImage(file: MultipartFile): String
}
