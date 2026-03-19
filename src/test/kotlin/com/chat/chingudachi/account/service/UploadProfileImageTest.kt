package com.chat.chingudachi.account.service

import com.chat.chingudachi.account.infra.FileData
import com.chat.chingudachi.core.domain.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream

class UploadProfileImageTest : DescribeSpec() {
    private val uploadService = UploadService()

    private fun fileData(
        contentType: String = "image/jpeg",
        size: Long = 1024L,
    ) = FileData(
        inputStream = ByteArrayInputStream(ByteArray(size.toInt())),
        contentType = contentType,
        size = size,
    )

    init {
        describe("uploadProfileImage") {
            context("유효한 이미지 파일") {
                it("DEFAULT URL을 반환한다 (stub)") {
                    val file = fileData()

                    val result = uploadService.uploadProfileImage(file)

                    result shouldBe "DEFAULT"
                }
            }

            context("파일 크기가 5MB를 초과하면") {
                it("BadRequestException을 던진다") {
                    val oversizedFile = fileData(size = 6L * 1024 * 1024)

                    shouldThrow<BadRequestException> {
                        uploadService.uploadProfileImage(oversizedFile)
                    }
                }
            }

            context("허용되지 않는 포맷이면") {
                it("BadRequestException을 던진다") {
                    val gifFile = fileData(contentType = "image/gif")

                    shouldThrow<BadRequestException> {
                        uploadService.uploadProfileImage(gifFile)
                    }
                }
            }

            context("PNG 파일") {
                it("DEFAULT URL을 반환한다 (stub)") {
                    val pngFile = fileData(contentType = "image/png")

                    val result = uploadService.uploadProfileImage(pngFile)

                    result shouldBe "DEFAULT"
                }
            }

            context("WebP 파일") {
                it("DEFAULT URL을 반환한다 (stub)") {
                    val webpFile = fileData(contentType = "image/webp")

                    val result = uploadService.uploadProfileImage(webpFile)

                    result shouldBe "DEFAULT"
                }
            }
        }
    }
}
