package com.example.kpappercutting.config

import org.springframework.stereotype.Component
import java.io.File

@Component
class UploadStorageProperties {
    private val baseUploadDir: File =
        File(System.getenv("UPLOAD_BASE_PATH") ?: "/home/ubuntu/ppcut").absoluteFile

    val imageDir: File = File(baseUploadDir, "kp_uploads")
    val videoDir: File = File(baseUploadDir, "kp_videos")
    val postDraftDir: File = File(baseUploadDir, "kp_drafts")
    val userDraftDir: File = File(baseUploadDir, "kp_user_drafts")
    val customPatternDir: File = File(baseUploadDir, "kp_custom_patterns")

    fun resourceLocation(dir: File): String {
        val uri = dir.toURI().toString()
        return if (uri.endsWith("/")) uri else "$uri/"
    }
}
