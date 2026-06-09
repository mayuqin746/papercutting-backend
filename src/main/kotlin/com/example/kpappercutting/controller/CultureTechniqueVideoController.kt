package com.example.kpappercutting.controller

import com.example.kpappercutting.model.CultureTechniqueVideo
import com.example.kpappercutting.repository.CultureTechniqueVideoRepository
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/culture/techniques")
class CultureTechniqueVideoController(
    private val techniqueVideoRepository: CultureTechniqueVideoRepository
) {
    @GetMapping
    fun listPublicTechniqueVideos(): List<CultureTechniqueVideoDto> {
        return techniqueVideoRepository
            .findByEnabledTrueOrderBySortOrderDescIdDesc()
            .map(::toDto)
    }

    @GetMapping("/admin")
    fun listAdminTechniqueVideos(
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<CultureTechniqueVideoDto> {
        val pageable = adminPageRequest(
            page,
            size,
            Sort.by(Sort.Order.desc("sortOrder"), Sort.Order.desc("id"))
        )
        val pageResult = if (status.trim().equals("all", ignoreCase = true) && keyword.isBlank()) {
            techniqueVideoRepository.findAllByOrderBySortOrderDescIdDesc(pageable)
        } else {
            techniqueVideoRepository.findAll(buildTechniqueVideoSpecification(status, keyword), pageable)
        }
        return pageResult.toAdminPageResponse(::toDto)
    }

    @PostMapping("/admin")
    fun createAdminTechniqueVideo(@RequestBody request: CultureTechniqueVideoRequest): ResponseEntity<Any> {
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val video = CultureTechniqueVideo(
            title = request.title.trim(),
            description = request.description.trim(),
            videoUrl = request.videoUrl.trim(),
            sortOrder = request.sortOrder,
            enabled = request.enabled
        )
        return ResponseEntity.ok(toDto(techniqueVideoRepository.save(video)))
    }

    @PutMapping("/admin/{videoId}")
    fun updateAdminTechniqueVideo(
        @PathVariable videoId: Long,
        @RequestBody request: CultureTechniqueVideoRequest
    ): ResponseEntity<Any> {
        val existing = techniqueVideoRepository.findById(videoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val updated = existing.copy(
            title = request.title.trim(),
            description = request.description.trim(),
            videoUrl = request.videoUrl.trim(),
            sortOrder = request.sortOrder,
            enabled = request.enabled,
            updateTime = LocalDateTime.now()
        )
        return ResponseEntity.ok(toDto(techniqueVideoRepository.save(updated)))
    }

    @PatchMapping("/admin/{videoId}/enabled")
    fun updateTechniqueVideoEnabled(
        @PathVariable videoId: Long,
        @RequestBody request: CultureTechniqueVideoEnabledRequest
    ): ResponseEntity<Any> {
        val video = techniqueVideoRepository.findById(videoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        video.enabled = request.enabled
        video.updateTime = LocalDateTime.now()
        return ResponseEntity.ok(toDto(techniqueVideoRepository.save(video)))
    }

    @DeleteMapping("/admin/{videoId}")
    fun deleteAdminTechniqueVideo(@PathVariable videoId: Long): ResponseEntity<Any> {
        if (!techniqueVideoRepository.existsById(videoId)) return ResponseEntity.notFound().build()
        techniqueVideoRepository.deleteById(videoId)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    private fun validateRequest(request: CultureTechniqueVideoRequest): String? {
        if (request.title.isBlank()) return "技法标题不能为空"
        val videoUrl = request.videoUrl.trim()
        if (!videoUrl.startsWith("/videos/") && !videoUrl.startsWith("http")) return "请上传技法视频"
        return null
    }

    private fun buildTechniqueVideoSpecification(
        status: String,
        keyword: String
    ): Specification<CultureTechniqueVideo> {
        val normalizedEnabled = when (status.trim().uppercase()) {
            "ENABLED" -> true
            "DISABLED" -> false
            else -> null
        }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            normalizedEnabled?.let {
                predicates += criteriaBuilder.equal(root.get<Boolean>("enabled"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue)
                )
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun toDto(video: CultureTechniqueVideo): CultureTechniqueVideoDto {
        return CultureTechniqueVideoDto(
            id = video.id,
            title = video.title,
            description = video.description,
            videoUrl = video.videoUrl,
            sortOrder = video.sortOrder,
            enabled = video.enabled,
            createTime = video.createTime,
            updateTime = video.updateTime
        )
    }
}

data class CultureTechniqueVideoRequest(
    val title: String,
    val description: String = "",
    val videoUrl: String,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)

data class CultureTechniqueVideoEnabledRequest(
    val enabled: Boolean
)

data class CultureTechniqueVideoDto(
    val id: Long,
    val title: String,
    val description: String,
    val videoUrl: String,
    val sortOrder: Int,
    val enabled: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)
