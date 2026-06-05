package com.example.kpappercutting.controller

import com.example.kpappercutting.model.OPERATION_BANNER_CULTURE
import com.example.kpappercutting.model.OPERATION_BANNER_HOME
import com.example.kpappercutting.model.OPERATION_BANNER_PLACEMENTS
import com.example.kpappercutting.model.OperationBanner
import com.example.kpappercutting.repository.OperationBannerRepository
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/operation-banners")
class OperationBannerController(
    private val bannerRepository: OperationBannerRepository
) {
    @GetMapping
    fun listPublicBanners(
        @RequestParam(defaultValue = OPERATION_BANNER_HOME) placement: String
    ): List<OperationBannerDto> {
        val normalizedPlacement = normalizePlacement(placement)
        return bannerRepository
            .findByPlacementAndEnabledTrueOrderBySortOrderDescIdDesc(normalizedPlacement)
            .let { banners ->
                if (normalizedPlacement == OPERATION_BANNER_CULTURE) banners.take(1) else banners
            }
            .map(::toDto)
    }

    @GetMapping("/admin")
    fun listAdminBanners(
        @RequestParam(defaultValue = "all") placement: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<OperationBannerDto> {
        val pageable = adminPageRequest(
            page,
            size,
            Sort.by(Sort.Order.asc("placement"), Sort.Order.desc("sortOrder"), Sort.Order.desc("id"))
        )
        val pageResult = if (placement.trim().equals("all", ignoreCase = true)) {
            bannerRepository.findAllByOrderByPlacementAscSortOrderDescIdDesc(pageable)
        } else {
            bannerRepository.findByPlacement(normalizePlacement(placement), pageable)
        }
        return pageResult.toAdminPageResponse(::toDto)
    }

    @PostMapping("/admin")
    fun createAdminBanner(@RequestBody request: OperationBannerRequest): ResponseEntity<Any> {
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)
        val banner = OperationBanner(
            placement = normalizePlacement(request.placement),
            imageUrl = request.imageUrl.trim(),
            sortOrder = request.sortOrder,
            enabled = request.enabled
        )
        return ResponseEntity.ok(toDto(bannerRepository.save(banner)))
    }

    @PutMapping("/admin/{bannerId}")
    fun updateAdminBanner(
        @PathVariable bannerId: Long,
        @RequestBody request: OperationBannerRequest
    ): ResponseEntity<Any> {
        val existing = bannerRepository.findById(bannerId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val validationError = validateRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)
        val updated = existing.copy(
            placement = normalizePlacement(request.placement),
            imageUrl = request.imageUrl.trim(),
            sortOrder = request.sortOrder,
            enabled = request.enabled,
            updateTime = LocalDateTime.now()
        )
        return ResponseEntity.ok(toDto(bannerRepository.save(updated)))
    }

    @PatchMapping("/admin/{bannerId}/enabled")
    fun updateBannerEnabled(
        @PathVariable bannerId: Long,
        @RequestBody request: OperationBannerEnabledRequest
    ): ResponseEntity<Any> {
        val banner = bannerRepository.findById(bannerId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        banner.enabled = request.enabled
        banner.updateTime = LocalDateTime.now()
        return ResponseEntity.ok(toDto(bannerRepository.save(banner)))
    }

    @DeleteMapping("/admin/{bannerId}")
    fun deleteAdminBanner(@PathVariable bannerId: Long): ResponseEntity<Any> {
        if (!bannerRepository.existsById(bannerId)) return ResponseEntity.notFound().build()
        bannerRepository.deleteById(bannerId)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    private fun validateRequest(request: OperationBannerRequest): String? {
        if (request.placement.trim().uppercase() !in OPERATION_BANNER_PLACEMENTS) return "Banner位置不合法"
        val imageUrl = request.imageUrl.trim()
        if (!imageUrl.startsWith("/images/") && !imageUrl.startsWith("http")) return "Banner图片不能为空"
        return null
    }

    private fun normalizePlacement(placement: String): String {
        return placement.trim().uppercase().takeIf { it in OPERATION_BANNER_PLACEMENTS }
            ?: OPERATION_BANNER_HOME
    }

    private fun toDto(banner: OperationBanner): OperationBannerDto {
        return OperationBannerDto(
            id = banner.id,
            placement = banner.placement,
            imageUrl = banner.imageUrl,
            sortOrder = banner.sortOrder,
            enabled = banner.enabled,
            createTime = banner.createTime,
            updateTime = banner.updateTime
        )
    }
}

data class OperationBannerRequest(
    val placement: String = OPERATION_BANNER_HOME,
    val imageUrl: String,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)

data class OperationBannerEnabledRequest(
    val enabled: Boolean
)

data class OperationBannerDto(
    val id: Long,
    val placement: String,
    val imageUrl: String,
    val sortOrder: Int,
    val enabled: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)
