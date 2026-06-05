package com.example.kpappercutting.controller

import com.example.kpappercutting.model.UserCustomPattern
import com.example.kpappercutting.repository.UserCustomPatternRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/custom-patterns")
class CustomPatternController(
    private val customPatternRepository: UserCustomPatternRepository,
    private val userRepository: UserRepository
) {
    @GetMapping
    fun listPatterns(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        if (!userRepository.existsById(authUserId)) {
            return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))
        }
        return ResponseEntity.ok(
            customPatternRepository.findByUserIdOrderByUpdatedAtDesc(authUserId).map(::toResponse)
        )
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun savePattern(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) patternId: String?,
        @RequestParam displayName: String,
        @RequestParam normalizedPathJson: String,
        @RequestParam("image", required = false) imageFile: MultipartFile?
    ): ResponseEntity<Any> {
        if (normalizedPathJson.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "图案路径不能为空"))
        }
        val authUserId = request.currentUserId()
        val user = userRepository.findById(authUserId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))

        val resolvedPatternId = patternId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val existing = customPatternRepository.findByPatternIdAndUserId(resolvedPatternId, authUserId)
        val patternDir = File(CUSTOM_PATTERN_ROOT, "$authUserId/$resolvedPatternId").apply { mkdirs() }
        val imageUrl = if (imageFile != null && !imageFile.isEmpty) {
            val extension = when {
                imageFile.contentType?.contains("png") == true -> "png"
                imageFile.contentType?.contains("webp") == true -> "webp"
                else -> "jpg"
            }
            val destination = File(patternDir, "pattern.$extension")
            imageFile.transferTo(destination)
            "/custom-patterns/$authUserId/$resolvedPatternId/pattern.$extension"
        } else {
            existing?.imageUrl ?: ""
        }

        val now = System.currentTimeMillis()
        val saved = customPatternRepository.save(
            UserCustomPattern(
                patternId = resolvedPatternId,
                user = user,
                displayName = displayName.trim().takeIf { it.isNotBlank() }
                    ?: existing?.displayName
                    ?: "自定义图案",
                imageUrl = imageUrl,
                thumbnailUrl = imageUrl,
                normalizedPathJson = normalizedPathJson,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        return ResponseEntity.ok(toResponse(saved))
    }

    @DeleteMapping("/{patternId}")
    fun deletePattern(
        request: HttpServletRequest,
        @PathVariable patternId: String,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val existing = customPatternRepository.findByPatternIdAndUserId(patternId, authUserId)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "图案不存在"))
        customPatternRepository.delete(existing)
        File(CUSTOM_PATTERN_ROOT, "$authUserId/$patternId").deleteRecursively()
        return ResponseEntity.ok(mapOf("message" to "删除成功"))
    }

    private fun toResponse(pattern: UserCustomPattern): UserCustomPatternResponse {
        return UserCustomPatternResponse(
            patternId = pattern.patternId,
            displayName = pattern.displayName,
            imageUrl = pattern.imageUrl,
            thumbnailUrl = pattern.thumbnailUrl,
            normalizedPathJson = pattern.normalizedPathJson,
            createdAt = pattern.createdAt,
            updatedAt = pattern.updatedAt
        )
    }

    data class UserCustomPatternResponse(
        val patternId: String,
        val displayName: String,
        val imageUrl: String,
        val thumbnailUrl: String,
        val normalizedPathJson: String,
        val createdAt: Long,
        val updatedAt: Long
    )

    companion object {
        private const val CUSTOM_PATTERN_ROOT = "/home/ubuntu/kp_custom_patterns"
    }
}
