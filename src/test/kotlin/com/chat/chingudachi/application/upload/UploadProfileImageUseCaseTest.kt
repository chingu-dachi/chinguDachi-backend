package com.chat.chingudachi.application.upload

import com.chat.chingudachi.application.upload.port.FileData
import com.chat.chingudachi.application.upload.port.FileStore
import com.chat.chingudachi.domain.common.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream

class UploadProfileImageUseCaseTest : DescribeSpec() {
    private val fileStore = mockk<FileStore>()
    private val useCase = UploadProfileImageUseCase(fileStore)

    private fun fileData(
        contentType: String = "image/jpeg",
        size: Long = 1024L,
    ) = FileData(
        inputStream = ByteArrayInputStream(ByteArray(size.toInt())),
        contentType = contentType,
        size = size,
    )

    init {
        describe("execute") {
            context("유효한 이미지 파일") {
                it("FileStore에 위임하고 URL을 반환한다") {
                    val file = fileData()
                    val expectedUrl = "http://localhost:8080/uploads/profile-images/test.jpg"
                    every { fileStore.saveProfileImage(any()) } returns expectedUrl

                    val result = useCase.execute(file)

                    result shouldBe expectedUrl
                    verify(exactly = 1) { fileStore.saveProfileImage(any()) }
                }
            }

            context("파일 크기가 5MB를 초과하면") {
                it("BadRequestException을 던진다") {
                    val oversizedFile = fileData(size = 6L * 1024 * 1024)

                    shouldThrow<BadRequestException> {
                        useCase.execute(oversizedFile)
                    }
                }
            }

            context("허용되지 않는 포맷이면") {
                it("BadRequestException을 던진다") {
                    val gifFile = fileData(contentType = "image/gif")

                    shouldThrow<BadRequestException> {
                        useCase.execute(gifFile)
                    }
                }
            }

            context("PNG 파일") {
                it("정상 처리된다") {
                    val pngFile = fileData(contentType = "image/png")
                    every { fileStore.saveProfileImage(any()) } returns "http://localhost/test.png"

                    val result = useCase.execute(pngFile)

                    result shouldBe "http://localhost/test.png"
                }
            }

            context("WebP 파일") {
                it("정상 처리된다") {
                    val webpFile = fileData(contentType = "image/webp")
                    every { fileStore.saveProfileImage(any()) } returns "http://localhost/test.webp"

                    val result = useCase.execute(webpFile)

                    result shouldBe "http://localhost/test.webp"
                }
            }
        }
    }
}
