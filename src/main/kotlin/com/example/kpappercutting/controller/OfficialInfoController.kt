package com.example.kpappercutting.controller

import com.example.kpappercutting.config.UploadStorageProperties
import com.example.kpappercutting.model.OFFICIAL_INFO_CATEGORIES
import com.example.kpappercutting.model.OFFICIAL_INFO_STATUS_ARCHIVED
import com.example.kpappercutting.model.OFFICIAL_INFO_STATUS_PUBLISHED
import com.example.kpappercutting.model.OfficialInfo
import com.example.kpappercutting.repository.OfficialInfoRepository
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/official-info")
class OfficialInfoController(
    private val officialInfoRepository: OfficialInfoRepository,
    private val uploadStorage: UploadStorageProperties
) {
    @GetMapping("/home")
    fun getHomeOfficialInfo(): List<OfficialInfoDto> {
        return officialInfoRepository
            .findByStatusOrderBySortOrderDescPublishDateDescIdDesc(OFFICIAL_INFO_STATUS_PUBLISHED)
            .map(::toDto)
    }

    @GetMapping("/{infoId}")
    fun getOfficialInfoDetail(@PathVariable infoId: Long): ResponseEntity<Any> {
        val info = officialInfoRepository.findById(infoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (info.status != OFFICIAL_INFO_STATUS_PUBLISHED) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(toDto(info))
    }

    @GetMapping("/admin")
    fun getAdminOfficialInfo(
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "all") category: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<OfficialInfoDto> {
        val pageable = adminPageRequest(
            page,
            size,
            Sort.by(Sort.Order.desc("sortOrder"), Sort.Order.desc("publishDate"), Sort.Order.desc("id"))
        )
        return officialInfoRepository
            .findAll(buildOfficialInfoSpecification(status, category, keyword), pageable)
            .toAdminPageResponse(::toDto)
    }

    @PostMapping("/admin")
    fun createAdminOfficialInfo(@RequestBody request: OfficialInfoRequest): ResponseEntity<Any> {
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val info = OfficialInfo(
            category = normalizeCategory(request.category),
            title = request.title.trim(),
            summary = request.summary.trim(),
            content = request.content.trim(),
            coverImageUrl = request.coverImageUrl.trim(),
            videoUrl = request.videoUrl.trim(),
            publishDate = request.publishDate,
            status = normalizeStatus(request.status),
            sortOrder = request.sortOrder
        )

        return ResponseEntity.ok(toDto(officialInfoRepository.save(info)))
    }

    @PutMapping("/admin/{infoId}")
    fun updateAdminOfficialInfo(
        @PathVariable infoId: Long,
        @RequestBody request: OfficialInfoRequest
    ): ResponseEntity<Any> {
        val existing = officialInfoRepository.findById(infoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val updated = existing.copy(
            category = normalizeCategory(request.category),
            title = request.title.trim(),
            summary = request.summary.trim(),
            content = request.content.trim(),
            coverImageUrl = request.coverImageUrl.trim(),
            videoUrl = request.videoUrl.trim(),
            publishDate = request.publishDate,
            status = normalizeStatus(request.status),
            sortOrder = request.sortOrder,
            updateTime = LocalDateTime.now()
        )

        return ResponseEntity.ok(toDto(officialInfoRepository.save(updated)))
    }

    @PostMapping("/admin/{infoId}/status")
    fun updateAdminOfficialInfoStatus(
        @PathVariable infoId: Long,
        @RequestBody request: OfficialInfoStatusRequest
    ): ResponseEntity<Any> {
        val existing = officialInfoRepository.findById(infoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val updated = existing.copy(
            status = normalizeStatus(request.status),
            updateTime = LocalDateTime.now()
        )
        return ResponseEntity.ok(toDto(officialInfoRepository.save(updated)))
    }

    @DeleteMapping("/admin/{infoId}")
    fun deleteAdminOfficialInfo(@PathVariable infoId: Long): ResponseEntity<Any> {
        if (!officialInfoRepository.existsById(infoId)) return ResponseEntity.notFound().build()
        officialInfoRepository.deleteById(infoId)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/admin/upload")
    fun uploadOfficialInfoCover(@RequestParam("image") file: MultipartFile): ResponseEntity<Any> {
        return saveUploadedImage(file, uploadStorage.imageDir)
    }

    @PostMapping("/admin/upload-video")
    fun uploadOfficialInfoVideo(@RequestParam("video") file: MultipartFile): ResponseEntity<Any> {
        return saveUploadedVideo(file, uploadStorage.videoDir)
    }

    private fun validateRequest(request: OfficialInfoRequest): String? {
        if (request.title.isBlank()) return "标题不能为空"
        if (request.summary.isBlank()) return "摘要不能为空"
        if (request.content.isBlank()) return "正文不能为空"
        if (!request.coverImageUrl.trim().startsWith("/images/") && !request.coverImageUrl.trim().startsWith("http")) {
            return "封面图不能为空"
        }
        val videoUrl = request.videoUrl.trim()
        if (videoUrl.isNotBlank() && !videoUrl.startsWith("/videos/")) {
            return "视频请通过后台上传"
        }
        return null
    }

    private fun normalizeCategory(category: String): String {
        return category.trim().uppercase().takeIf { it in OFFICIAL_INFO_CATEGORIES } ?: OFFICIAL_INFO_CATEGORIES.first()
    }

    private fun normalizeStatus(status: String): String {
        return when (status.trim().uppercase()) {
            OFFICIAL_INFO_STATUS_ARCHIVED -> OFFICIAL_INFO_STATUS_ARCHIVED
            else -> OFFICIAL_INFO_STATUS_PUBLISHED
        }
    }

    private fun toDto(info: OfficialInfo): OfficialInfoDto {
        return OfficialInfoDto(
            id = info.id,
            category = info.category,
            title = info.title,
            summary = info.summary,
            content = info.content,
            coverImageUrl = info.coverImageUrl,
            videoUrl = info.videoUrl,
            publishDate = info.publishDate,
            status = info.status,
            sortOrder = info.sortOrder,
            createTime = info.createTime,
            updateTime = info.updateTime
        )
    }

    private fun buildOfficialInfoSpecification(
        status: String,
        category: String,
        keyword: String
    ): Specification<OfficialInfo> {
        val normalizedStatus = status.trim().uppercase().takeIf {
            it in setOf(OFFICIAL_INFO_STATUS_PUBLISHED, OFFICIAL_INFO_STATUS_ARCHIVED)
        }
        val normalizedCategory = category.trim().uppercase().takeIf { it in OFFICIAL_INFO_CATEGORIES }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            normalizedStatus?.let {
                predicates += criteriaBuilder.equal(root.get<String>("status"), it)
            }
            normalizedCategory?.let {
                predicates += criteriaBuilder.equal(root.get<String>("category"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}

data class OfficialInfoRequest(
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val coverImageUrl: String,
    val videoUrl: String = "",
    val publishDate: LocalDate = LocalDate.now(),
    val status: String = OFFICIAL_INFO_STATUS_PUBLISHED,
    val sortOrder: Int = 0
)

data class OfficialInfoStatusRequest(
    val status: String
)

data class OfficialInfoDto(
    val id: Long,
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val coverImageUrl: String,
    val videoUrl: String,
    val publishDate: LocalDate,
    val status: String,
    val sortOrder: Int,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)
