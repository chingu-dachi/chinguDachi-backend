package com.chat.chingudachi.presentation.upload

import com.chat.chingudachi.application.upload.UploadProfileImageUseCase
import com.chat.chingudachi.application.upload.port.FileData
import com.chat.chingudachi.presentation.common.AuthAccountId
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/uploads")
class UploadController(
    private val uploadProfileImageUseCase: UploadProfileImageUseCase,
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
        val imageUrl = uploadProfileImageUseCase.execute(fileData)
        return UploadResponse(imageUrl)
    }
}

data class UploadResponse(
    val imageUrl: String,
)
