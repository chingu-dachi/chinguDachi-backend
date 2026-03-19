package com.chat.chingudachi.account.controller

import com.chat.chingudachi.account.infra.FileData
import com.chat.chingudachi.account.service.UploadService
import com.chat.chingudachi.core.presentation.AuthAccountId
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/uploads")
class UploadController(
    private val uploadService: UploadService,
) {
    @PostMapping("/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfileImage(
        @AuthAccountId accountId: Long,
        @RequestPart file: MultipartFile,
    ): UploadResponse {
        val fileData = FileData(
            inputStream = file.inputStream,
            contentType = file.contentType ?: "",
            size = file.size,
        )
        val imageUrl = uploadService.uploadProfileImage(fileData)
        return UploadResponse(imageUrl)
    }
}

data class UploadResponse(
    val imageUrl: String,
)
