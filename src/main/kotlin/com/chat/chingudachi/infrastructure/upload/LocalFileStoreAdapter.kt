package com.chat.chingudachi.infrastructure.upload

import com.chat.chingudachi.application.upload.port.FileStore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
@EnableConfigurationProperties(UploadProperties::class)
class LocalFileStoreAdapter(
    private val uploadProperties: UploadProperties,
) : FileStore {

    override fun saveProfileImage(file: MultipartFile): String {
        val directory = Path.of(uploadProperties.directory)
        Files.createDirectories(directory)

        val extension = extractExtension(file.contentType)
        val filename = "${UUID.randomUUID()}.$extension"
        val targetPath = directory.resolve(filename)

        file.transferTo(targetPath)

        return "${uploadProperties.baseUrl}/$filename"
    }

    private fun extractExtension(contentType: String?): String =
        when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
}
