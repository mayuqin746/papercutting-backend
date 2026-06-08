package com.example.kpappercutting.controller

import com.example.kpappercutting.config.UploadStorageProperties
import com.example.kpappercutting.model.UserDraft
import com.example.kpappercutting.repository.UserDraftRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/drafts")
class DraftController(
    private val draftRepository: UserDraftRepository,
    private val userRepository: UserRepository,
    private val uploadStorage: UploadStorageProperties
) {
    @GetMapping
    fun listDrafts(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        if (!userRepository.existsById(authUserId)) {
            return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))
        }
        return ResponseEntity.ok(draftRepository.findByUserIdOrderByUpdatedAtDesc(authUserId).map(::toResponse))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createDraft(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) draftId: String?,
        @RequestParam title: String,
        @RequestParam paperColor: Int,
        @RequestParam foldMode: String,
        @RequestParam canvasMode: String,
        @RequestParam isFolded: Boolean,
        @RequestParam("draft") draftFile: MultipartFile,
        @RequestParam("thumbnail") thumbnailFile: MultipartFile
    ): ResponseEntity<Any> {
        val resolvedDraftId = draftId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        return saveDraft(
            userId = request.currentUserId(),
            draftId = resolvedDraftId,
            title = title,
            paperColor = paperColor,
            foldMode = foldMode,
            canvasMode = canvasMode,
            isFolded = isFolded,
            draftFile = draftFile,
            thumbnailFile = thumbnailFile,
            allowCreate = true
        )
    }

    @PutMapping("/{draftId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateDraft(
        request: HttpServletRequest,
        @PathVariable draftId: String,
        @RequestParam(required = false) userId: Long?,
        @RequestParam title: String,
        @RequestParam paperColor: Int,
        @RequestParam foldMode: String,
        @RequestParam canvasMode: String,
        @RequestParam isFolded: Boolean,
        @RequestParam("draft") draftFile: MultipartFile,
        @RequestParam("thumbnail") thumbnailFile: MultipartFile
    ): ResponseEntity<Any> {
        return saveDraft(
            userId = request.currentUserId(),
            draftId = draftId,
            title = title,
            paperColor = paperColor,
            foldMode = foldMode,
            canvasMode = canvasMode,
            isFolded = isFolded,
            draftFile = draftFile,
            thumbnailFile = thumbnailFile,
            allowCreate = true
        )
    }

    @GetMapping("/{draftId}/file")
    fun downloadDraft(
        request: HttpServletRequest,
        @PathVariable draftId: String,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val draft = draftRepository.findByDraftIdAndUserId(draftId, authUserId)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "草稿不存在"))
        val file = File(uploadStorage.userDraftDir, "${draft.user?.id}/${draft.draftId}/draft.zip")
        if (!file.exists()) {
            return ResponseEntity.status(404).body(mapOf("message" to "草稿文件不存在"))
        }
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(FileSystemResource(file))
    }

    @PatchMapping("/{draftId}/rename")
    fun renameDraft(
        request: HttpServletRequest,
        @PathVariable draftId: String,
        @RequestParam(required = false) userId: Long?,
        @RequestParam title: String
    ): ResponseEntity<Any> {
        val existing = draftRepository.findByDraftIdAndUserId(draftId, request.currentUserId())
            ?: return ResponseEntity.status(404).body(mapOf("message" to "草稿不存在"))
        val nextTitle = title.trim().takeIf { it.isNotBlank() } ?: existing.title
        val saved = draftRepository.save(
            existing.copy(
                title = nextTitle,
                updatedAt = System.currentTimeMillis()
            )
        )
        return ResponseEntity.ok(toResponse(saved))
    }

    @DeleteMapping("/{draftId}")
    fun deleteDraft(
        request: HttpServletRequest,
        @PathVariable draftId: String,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val existing = draftRepository.findByDraftIdAndUserId(draftId, authUserId)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "草稿不存在"))
        draftRepository.delete(existing)
        File(uploadStorage.userDraftDir, "$authUserId/$draftId").deleteRecursively()
        return ResponseEntity.ok(mapOf("message" to "删除成功"))
    }

    private fun saveDraft(
        userId: Long,
        draftId: String,
        title: String,
        paperColor: Int,
        foldMode: String,
        canvasMode: String,
        isFolded: Boolean,
        draftFile: MultipartFile,
        thumbnailFile: MultipartFile,
        allowCreate: Boolean
    ): ResponseEntity<Any> {
        if (draftFile.isEmpty || thumbnailFile.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("message" to "草稿文件不能为空"))
        }
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))
        val existing = draftRepository.findByDraftIdAndUserId(draftId, userId)
        if (existing == null && !allowCreate) {
            return ResponseEntity.status(404).body(mapOf("message" to "草稿不存在"))
        }

        val draftDir = File(uploadStorage.userDraftDir, "$userId/$draftId").apply { mkdirs() }
        val draftDest = File(draftDir, "draft.zip")
        val thumbnailDest = File(draftDir, "thumbnail.png")
        draftFile.transferTo(draftDest)
        thumbnailFile.transferTo(thumbnailDest)

        val now = System.currentTimeMillis()
        val saved = draftRepository.save(
            UserDraft(
                draftId = draftId,
                user = user,
                title = title.trim().takeIf { it.isNotBlank() } ?: existing?.title ?: "未命名草稿",
                thumbnailUrl = "/user-drafts/$userId/$draftId/thumbnail.png",
                draftUrl = "/user-drafts/$userId/$draftId/draft.zip",
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                paperColor = paperColor,
                foldMode = foldMode,
                canvasMode = canvasMode,
                isFolded = isFolded
            )
        )
        return ResponseEntity.ok(toResponse(saved))
    }

    private fun toResponse(draft: UserDraft): UserDraftResponse {
        return UserDraftResponse(
            draftId = draft.draftId,
            title = draft.title,
            thumbnailUrl = draft.thumbnailUrl,
            draftUrl = draft.draftUrl,
            createdAt = draft.createdAt,
            updatedAt = draft.updatedAt,
            paperColor = draft.paperColor,
            foldMode = draft.foldMode,
            canvasMode = draft.canvasMode,
            isFolded = draft.isFolded
        )
    }

    data class UserDraftResponse(
        val draftId: String,
        val title: String,
        val thumbnailUrl: String,
        val draftUrl: String,
        val createdAt: Long,
        val updatedAt: Long,
        val paperColor: Int,
        val foldMode: String,
        val canvasMode: String,
        val isFolded: Boolean
    )
}
