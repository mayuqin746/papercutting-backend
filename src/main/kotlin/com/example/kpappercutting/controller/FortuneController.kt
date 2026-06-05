package com.example.kpappercutting.controller

import com.example.kpappercutting.model.FortuneCard
import com.example.kpappercutting.model.FortuneCardCollection
import com.example.kpappercutting.repository.FortuneCardCollectionRepository
import com.example.kpappercutting.repository.FortuneCardRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import com.example.kpappercutting.security.currentUserIdOrNull
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
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
    private val userRepository: UserRepository
) {
    private val appZone: ZoneId = ZoneId.of("Asia/Shanghai")

    @GetMapping("/today")
    fun getTodayFortune(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): FortuneHomeResponse {
        val todayCard = fortuneCardRepository.findByDisplayDate(LocalDate.now(appZone))
        val effectiveUserId = request.currentUserIdOrNull() ?: userId
        val collections = effectiveUserId?.let { collectionRepository.findByUserIdOrderByCollectTimeAsc(it) }.orEmpty()
        val collectedCardIds = collections.map { it.fortuneCardId }.toSet()
        val collectedCards = if (collectedCardIds.isEmpty()) {
            emptyList()
        } else {
            fortuneCardRepository.findAllById(collectedCardIds)
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
        val userId = request.currentUserId()
        val fortuneCardId = body["fortuneCardId"] ?: return ResponseEntity.badRequest().body("缺少福运卡ID")

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404).body("用户不存在")
        }
        if (!fortuneCardRepository.existsById(fortuneCardId)) {
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
    fun getAdminFortuneCards(): List<FortuneCardDto> {
        return fortuneCardRepository.findAllByOrderByDisplayDateDescIdDesc().map(::toFortuneCardDto)
    }

    @PostMapping("/admin")
    fun createAdminFortuneCard(@RequestBody request: FortuneCardRequest): ResponseEntity<Any> {
        val validationError = validateFortuneCardRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)
        val existing = fortuneCardRepository.findByDisplayDate(request.displayDate)
        if (existing != null) return ResponseEntity.badRequest().body("该展示日期已存在福运卡")

        val card = FortuneCard(
            displayDate = request.displayDate,
            patternImageUrl = request.patternImageUrl.trim(),
            lunarDate = request.lunarDate.trim(),
            solarTerm = request.solarTerm.trim(),
            suitableEvents = request.suitableEvents.trim()
        )

        return ResponseEntity.ok(toFortuneCardDto(fortuneCardRepository.save(card)))
    }

    @PutMapping("/admin/{fortuneCardId}")
    fun updateAdminFortuneCard(
        @PathVariable fortuneCardId: Long,
        @RequestBody request: FortuneCardRequest
    ): ResponseEntity<Any> {
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
            updateTime = LocalDateTime.now()
        )

        return ResponseEntity.ok(toFortuneCardDto(fortuneCardRepository.save(updated)))
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
            suitableEvents = card.suitableEvents
        )
    }
}

data class FortuneCardRequest(
    val displayDate: LocalDate,
    val patternImageUrl: String,
    val lunarDate: String,
    val solarTerm: String,
    val suitableEvents: String
)

data class FortuneCardDto(
    val id: Long,
    val displayDate: LocalDate,
    val patternImageUrl: String,
    val lunarDate: String,
    val solarTerm: String,
    val suitableEvents: String
)

data class FortuneHomeResponse(
    val todayCard: FortuneCardDto?,
    val isCollected: Boolean = false,
    val collectedCards: List<FortuneCardDto> = emptyList()
)
