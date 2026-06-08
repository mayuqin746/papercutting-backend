package com.example.kpappercutting.controller

import com.example.kpappercutting.config.UploadStorageProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("/api/admin/uploads")
class AdminUploadController(
    private val uploadStorage: UploadStorageProperties
) {
    @PostMapping("/images")
    fun uploadImage(@RequestParam("image") file: MultipartFile): ResponseEntity<Any> {
        return saveUploadedImage(file, uploadStorage.imageDir)
    }

    @PostMapping("/videos")
    fun uploadVideo(@RequestParam("video") file: MultipartFile): ResponseEntity<Any> {
        return saveUploadedVideo(file, uploadStorage.videoDir)
    }
}

fun saveUploadedImage(file: MultipartFile, uploadTargetDir: File): ResponseEntity<Any> {
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

        val uploadDir = uploadTargetDir.apply {
            if (!exists()) mkdirs()
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        file.transferTo(File(uploadDir, fileName))

        ResponseEntity.ok(mapOf("url" to "/images/$fileName"))
    } catch (e: Exception) {
        ResponseEntity.internalServerError().body(e.message)
    }
}

fun saveUploadedVideo(file: MultipartFile, uploadTargetDir: File): ResponseEntity<Any> {
    return try {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("message" to "上传文件不能为空"))
        }

        val extension = file.originalFilename
            ?.substringAfterLast(".", "mp4")
            ?.lowercase()
            ?: "mp4"
        val allowedExtensions = setOf("mp4", "m4v", "3gp")
        if (extension !in allowedExtensions) {
            return ResponseEntity.badRequest().body(mapOf("message" to "只支持 mp4、m4v、3gp 视频格式，建议使用 H.264/AAC 编码"))
        }

        val uploadDir = uploadTargetDir.apply {
            if (!exists()) mkdirs()
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        val destination = File(uploadDir, fileName)
        val tempSource = File(uploadDir, "${UUID.randomUUID()}-source.$extension")
        file.transferTo(tempSource)
        if (!tryOptimizeVideoForStreaming(tempSource, destination)) {
            tempSource.copyTo(destination, overwrite = true)
        }
        tempSource.delete()

        ResponseEntity.ok(mapOf("url" to "/videos/$fileName"))
    } catch (e: Exception) {
        ResponseEntity.internalServerError().body(e.message)
    }
}

private fun tryOptimizeVideoForStreaming(source: File, destination: File): Boolean {
    if (!source.extension.equals("mp4", ignoreCase = true) && !source.extension.equals("m4v", ignoreCase = true)) {
        return false
    }
    val tempOutput = File(destination.parentFile, "${destination.nameWithoutExtension}-faststart.${destination.extension}")
    return try {
        val process = ProcessBuilder(
            "ffmpeg",
            "-y",
            "-i",
            source.absolutePath,
            "-c",
            "copy",
            "-movflags",
            "+faststart",
            tempOutput.absolutePath
        )
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        val completed = process.waitFor(90, TimeUnit.SECONDS)
        if (completed && process.exitValue() == 0 && tempOutput.exists() && tempOutput.length() > 0L) {
            tempOutput.copyTo(destination, overwrite = true)
            tempOutput.delete()
            true
        } else {
            process.destroyForcibly()
            tempOutput.delete()
            false
        }
    } catch (_: Exception) {
        tempOutput.delete()
        false
    }
}
