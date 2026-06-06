package com.example.kpappercutting.controller

import com.example.kpappercutting.config.FortuneCardSchemaMigrator
import com.example.kpappercutting.model.FORTUNE_CARD_STATUS_ARCHIVED
import com.example.kpappercutting.model.FORTUNE_CARD_STATUS_PUBLISHED
import com.example.kpappercutting.model.FortuneCard
import com.example.kpappercutting.model.FortuneCardCollection
import com.example.kpappercutting.repository.FortuneCardCollectionRepository
import com.example.kpappercutting.repository.FortuneCardRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import com.example.kpappercutting.security.currentUserIdOrNull
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@CrossOrigin
@RestController
@RequestMapping("/api/fortune")
class FortuneController(
    private val fortuneCardRepository: FortuneCardRepository,
    private val collectionRepository: FortuneCardCollectionRepository,
    private val userRepository: UserRepository,
    private val schemaMigrator: FortuneCardSchemaMigrator
) {
    private val appZone: ZoneId = ZoneId.of("Asia/Shanghai")

    @GetMapping("/today")
    fun getTodayFortune(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): FortuneHomeResponse {
        schemaMigrator.ensureStatusColumn()
        val todayCard = fortuneCardRepository.findPublishedByDisplayDate(
            LocalDate.now(appZone),
            FORTUNE_CARD_STATUS_PUBLISHED
        )
        val effectiveUserId = request.currentUserIdOrNull() ?: userId
        val collections = effectiveUserId?.let { collectionRepository.findByUserIdOrderByCollectTimeAsc(it) }.orEmpty()
        val collectedCardIds = collections.map { it.fortuneCardId }.toSet()
        val collectedCards = if (collectedCardIds.isEmpty()) {
            emptyList()
        } else {
            fortuneCardRepository.findAllById(collectedCardIds)
                .filter { normalizeFortuneCardStatus(it.status) == FORTUNE_CARD_STATUS_PUBLISHED }
                .sortedBy { card -> collections.find { it.fortuneCardId == card.id }?.collectTime }
                .map(::toFortuneCardDto)
        }

        return FortuneHomeResponse(
            todayCard = todayCard?.let(::toFortuneCardDto),
            isCollected = todayCard?.id?.let { it in collectedCardIds } ?: false,
            collectedCards = collectedCards
        )
    }

    @PostMapping("/collect")
    fun collectFortune(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Long>
    ): ResponseEntity<Any> {
        schemaMigrator.ensureStatusColumn()
        val userId = request.currentUserId()
        val fortuneCardId = body["fortuneCardId"] ?: return ResponseEntity.badRequest().body("缺少福运卡ID")

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404).body("用户不存在")
        }
        val fortuneCard = fortuneCardRepository.findById(fortuneCardId).orElse(null)
        if (fortuneCard == null || normalizeFortuneCardStatus(fortuneCard.status) != FORTUNE_CARD_STATUS_PUBLISHED) {
            return ResponseEntity.status(404).body("福运卡不存在")
        }

        val existing = collectionRepository.findByUserIdAndFortuneCardId(userId, fortuneCardId)
        if (existing == null) {
            collectionRepository.save(
                FortuneCardCollection(
                    userId = userId,
                    fortuneCardId = fortuneCardId,
                    collectTime = LocalDateTime.now()
                )
            )
        }

        return ResponseEntity.ok(mapOf("success" to true))
    }

    @GetMapping("/admin")
    fun getAdminFortuneCards(
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<FortuneCardDto> {
        schemaMigrator.ensureStatusColumn()
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Order.desc("displayDate"), Sort.Order.desc("id")))
        return fortuneCardRepository
            .findAll(buildFortuneCardSpecification(status, keyword, fromDate, toDate), pageable)
            .toAdminPageResponse(::toFortuneCardDto)
    }

    @PostMapping("/admin")
    fun createAdminFortuneCard(@RequestBody request: FortuneCardRequest): ResponseEntity<Any> {
        schemaMigrator.ensureStatusColumn()
        val validationError = validateFortuneCardRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)
        val existing = fortuneCardRepository.findByDisplayDate(request.displayDate)
        if (existing != null) return ResponseEntity.badRequest().body("该展示日期已存在福运卡")

        val card = FortuneCard(
            displayDate = request.displayDate,
            patternImageUrl = request.patternImageUrl.trim(),
            lunarDate = request.lunarDate.trim(),
            solarTerm = request.solarTerm.trim(),
            suitableEvents = request.suitableEvents.trim(),
            status = normalizeFortuneCardStatus(request.status)
        )

        return ResponseEntity.ok(toFortuneCardDto(fortuneCardRepository.save(card)))
    }

    @PutMapping("/admin/{fortuneCardId}")
    fun updateAdminFortuneCard(
        @PathVariable fortuneCardId: Long,
        @RequestBody request: FortuneCardRequest
    ): ResponseEntity<Any> {
        schemaMigrator.ensureStatusColumn()
        val existing = fortuneCardRepository.findById(fortuneCardId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val validationError = validateFortuneCardRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)
        val sameDateCard = fortuneCardRepository.findByDisplayDate(request.displayDate)
        if (sameDateCard != null && sameDateCard.id != fortuneCardId) {
            return ResponseEntity.badRequest().body("该展示日期已存在福运卡")
        }

        val updated = existing.copy(
            displayDate = request.displayDate,
            patternImageUrl = request.patternImageUrl.trim(),
            lunarDate = request.lunarDate.trim(),
            solarTerm = request.solarTerm.trim(),
            suitableEvents = request.suitableEvents.trim(),
            status = normalizeFortuneCardStatus(request.status),
            updateTime = LocalDateTime.now()
        )

        return ResponseEntity.ok(toFortuneCardDto(fortuneCardRepository.save(updated)))
    }

    @PostMapping("/admin/{fortuneCardId}/status")
    fun updateAdminFortuneCardStatus(
        @PathVariable fortuneCardId: Long,
        @RequestBody request: FortuneCardStatusRequest
    ): ResponseEntity<Any> {
        schemaMigrator.ensureStatusColumn()
        val existing = fortuneCardRepository.findById(fortuneCardId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val updated = existing.copy(
            status = normalizeFortuneCardStatus(request.status),
            updateTime = LocalDateTime.now()
        )
        return ResponseEntity.ok(toFortuneCardDto(fortuneCardRepository.save(updated)))
    }

    @Transactional
    @DeleteMapping("/admin/{fortuneCardId}")
    fun deleteAdminFortuneCard(@PathVariable fortuneCardId: Long): ResponseEntity<Any> {
        schemaMigrator.ensureStatusColumn()
        if (!fortuneCardRepository.existsById(fortuneCardId)) {
            return ResponseEntity.notFound().build()
        }
        collectionRepository.deleteByFortuneCardId(fortuneCardId)
        fortuneCardRepository.deleteById(fortuneCardId)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    private fun validateFortuneCardRequest(request: FortuneCardRequest): String? {
        if (request.patternImageUrl.isBlank()) return "图案图片URL不能为空"
        if (request.lunarDate.isBlank()) return "农历日期不能为空"
        if (request.solarTerm.isBlank()) return "节气不能为空"
        if (request.suitableEvents.isBlank()) return "宜做事件不能为空"
        return null
    }

    private fun toFortuneCardDto(card: FortuneCard): FortuneCardDto {
        return FortuneCardDto(
            id = card.id,
            displayDate = card.displayDate,
            patternImageUrl = card.patternImageUrl,
            lunarDate = card.lunarDate,
            solarTerm = card.solarTerm,
            suitableEvents = card.suitableEvents,
            status = normalizeFortuneCardStatus(card.status)
        )
    }

    private fun normalizeFortuneCardStatus(status: String?): String {
        return when (status?.trim()?.uppercase()) {
            FORTUNE_CARD_STATUS_ARCHIVED -> FORTUNE_CARD_STATUS_ARCHIVED
            else -> FORTUNE_CARD_STATUS_PUBLISHED
        }
    }

    private fun buildFortuneCardSpecification(
        status: String,
        keyword: String,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Specification<FortuneCard> {
        val normalizedStatus = status.trim().uppercase().takeIf {
            it in setOf(FORTUNE_CARD_STATUS_PUBLISHED, FORTUNE_CARD_STATUS_ARCHIVED)
        }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            normalizedStatus?.let {
                if (it == FORTUNE_CARD_STATUS_PUBLISHED) {
                    predicates += criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get<String>("status")),
                        criteriaBuilder.equal(root.get<String>("status"), it)
                    )
                } else {
                    predicates += criteriaBuilder.equal(root.get<String>("status"), it)
                }
            }
            fromDate?.let {
                predicates += criteriaBuilder.greaterThanOrEqualTo(root.get("displayDate"), it)
            }
            toDate?.let {
                predicates += criteriaBuilder.lessThanOrEqualTo(root.get("displayDate"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lunarDate")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("solarTerm")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("suitableEvents")), likeValue),
                    criteriaBuilder.like(root.get<LocalDate>("displayDate").`as`(String::class.java), likeValue)
                )
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}

data class FortuneCardRequest(
    val displayDate: LocalDate,
    val patternImageUrl: String,
    val lunarDate: String,
    val solarTerm: String,
    val suitableEvents: String,
    val status: String = FORTUNE_CARD_STATUS_PUBLISHED
)

data class FortuneCardStatusRequest(
    val status: String
)

data class FortuneCardDto(
    val id: Long,
    val displayDate: LocalDate,
    val patternImageUrl: String,
    val lunarDate: String,
    val solarTerm: String,
    val suitableEvents: String,
    val status: String = FORTUNE_CARD_STATUS_PUBLISHED
)

data class FortuneHomeResponse(
    val todayCard: FortuneCardDto?,
    val isCollected: Boolean = false,
    val collectedCards: List<FortuneCardDto> = emptyList()
)
