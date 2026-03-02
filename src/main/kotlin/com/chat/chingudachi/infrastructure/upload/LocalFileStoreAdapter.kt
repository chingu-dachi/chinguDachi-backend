package com.chat.chingudachi.infrastructure.upload

import com.chat.chingudachi.application.upload.port.FileData
import com.chat.chingudachi.application.upload.port.FileStore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Component
@EnableConfigurationProperties(UploadProperties::class)
class LocalFileStoreAdapter(
    private val uploadProperties: UploadProperties,
) : FileStore {

    override fun saveProfileImage(file: FileData): String {
        val directory = Path.of(uploadProperties.directory)
        Files.createDirectories(directory)

        val extension = extractExtension(file.contentType)
        val filename = "${UUID.randomUUID()}.$extension"
        val targetPath = directory.resolve(filename)

        file.inputStream.use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        return "${uploadProperties.baseUrl}/$filename"
    }

    private fun extractExtension(contentType: String): String =
        when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> error("Unsupported content type: $contentType")
        }
}
