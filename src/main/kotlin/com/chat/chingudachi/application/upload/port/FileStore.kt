package com.chat.chingudachi.application.upload.port

import java.io.InputStream

data class FileData(
    val inputStream: InputStream,
    val contentType: String,
    val size: Long,
)

interface FileStore {
    fun saveProfileImage(file: FileData): String
}
