package com.chat.chingudachi.presentation.upload

import com.chat.chingudachi.application.upload.UploadProfileImageUseCase
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
    fun uploadProfileImage(@RequestPart file: MultipartFile): UploadResponse {
        val imageUrl = uploadProfileImageUseCase.execute(file)
        return UploadResponse(imageUrl)
    }
}

data class UploadResponse(
    val imageUrl: String,
)
