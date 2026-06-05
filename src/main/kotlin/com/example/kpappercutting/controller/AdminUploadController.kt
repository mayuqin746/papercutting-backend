package com.example.kpappercutting.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("/api/admin/uploads")
class AdminUploadController {
    @PostMapping("/images")
    fun uploadImage(@RequestParam("image") file: MultipartFile): ResponseEntity<Any> {
        return saveUploadedImage(file)
    }
}

fun saveUploadedImage(file: MultipartFile): ResponseEntity<Any> {
    return try {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("message" to "上传文件不能为空"))
        }

        val extension = file.originalFilename
            ?.substringAfterLast(".", "jpg")
            ?.lowercase()
            ?: "jpg"
        val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
        if (extension !in allowedExtensions) {
            return ResponseEntity.badRequest().body(mapOf("message" to "只支持 jpg、jpeg、png、webp、gif 图片格式"))
        }

        val uploadDir = File("/home/ubuntu/kp_uploads").apply {
            if (!exists()) mkdirs()
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        file.transferTo(File(uploadDir, fileName))

        ResponseEntity.ok(mapOf("url" to "/images/$fileName"))
    } catch (e: Exception) {
        ResponseEntity.internalServerError().body(e.message)
    }
}
