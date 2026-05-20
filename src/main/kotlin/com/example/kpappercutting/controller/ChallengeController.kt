package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Challenge
import com.example.kpappercutting.model.ChallengeAttempt
import com.example.kpappercutting.repository.ChallengeAttemptRepository
import com.example.kpappercutting.repository.ChallengeParticipantRepository
import com.example.kpappercutting.repository.ChallengeRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

const val CHALLENGE_STATUS_DRAFT = "DRAFT"
const val CHALLENGE_STATUS_PUBLISHED = "PUBLISHED"
const val CHALLENGE_STATUS_ARCHIVED = "ARCHIVED"

val BASE_POST_CATEGORIES = listOf(
    "窗花",
    "团花",
    "生肖",
    "人物",
    "动物",
    "植物",
    "节日",
    "传统纹样",
    "AI剪纸",
    "自由创作"
)

@CrossOrigin
@RestController
@RequestMapping("/api/challenges")
class ChallengeController(
    private val challengeRepository: ChallengeRepository,
    private val challengeAttemptRepository: ChallengeAttemptRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository,
    private val userRepository: UserRepository
) {
    @GetMapping("/current")
    fun getCurrentChallenge(@RequestParam(required = false) userId: Long?): ResponseEntity<Any> {
        val challenge = challengeRepository.findFirstByStatusOrderByStartTimeDescIdDesc(CHALLENGE_STATUS_PUBLISHED)
            ?: return ResponseEntity.ok(CurrentChallengeResponse(challenge = null))

        return ResponseEntity.ok(toCurrentChallengeResponse(challenge, userId))
    }

    @PostMapping("/{challengeId}/attempt")
    fun attemptChallenge(
        @PathVariable challengeId: Long,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val userId = (body["userId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body("缺少用户ID")

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404).body("用户不存在")
        }

        val challenge = challengeRepository.findById(challengeId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (challenge.status != CHALLENGE_STATUS_PUBLISHED || LocalDateTime.now().isAfter(challenge.deadline)) {
            return ResponseEntity.badRequest().body("挑战已截止")
        }

        val existing = challengeAttemptRepository.findByChallengeIdAndUserId(challengeId, userId)
        if (existing == null) {
            challengeAttemptRepository.save(
                ChallengeAttempt(
                    challengeId = challengeId,
                    userId = userId,
                    attemptTime = LocalDateTime.now()
                )
            )
        }

        return ResponseEntity.ok(toCurrentChallengeResponse(challenge, userId))
    }

    @GetMapping("/post-categories")
    fun getPostCategories(): List<String> {
        val now = LocalDateTime.now()
        val challengeTags = challengeRepository
            .findByStatusAndDeadlineAfterOrderByStartTimeDescIdDesc(CHALLENGE_STATUS_PUBLISHED, now)
            .map { it.challengeTag.trim() }
            .filter { it.isNotEmpty() }

        return (BASE_POST_CATEGORIES + challengeTags).distinct()
    }

    @GetMapping("/admin")
    fun getAdminChallenges(): List<AdminChallengeResponse> {
        return challengeRepository.findAllByOrderByStartTimeDescIdDesc().map(::toAdminChallengeResponse)
    }

    @PostMapping("/admin")
    fun createAdminChallenge(@RequestBody request: ChallengeRequest): ResponseEntity<Any> {
        val validationError = validateChallengeRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val challenge = Challenge(
            title = request.title.trim(),
            activityLabel = request.activityLabel.trim(),
            challengeTag = request.challengeTag.trim(),
            description = request.description.trim(),
            inspirationImageUrls = normalizeImageUrls(request.inspirationImageUrls),
            startTime = request.startTime,
            deadline = request.deadline,
            status = normalizeChallengeStatus(request.status)
        )

        return ResponseEntity.ok(toAdminChallengeResponse(challengeRepository.save(challenge)))
    }

    @PutMapping("/admin/{challengeId}")
    fun updateAdminChallenge(
        @PathVariable challengeId: Long,
        @RequestBody request: ChallengeRequest
    ): ResponseEntity<Any> {
        val existing = challengeRepository.findById(challengeId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val validationError = validateChallengeRequest(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val updated = existing.copy(
            title = request.title.trim(),
            activityLabel = request.activityLabel.trim(),
            challengeTag = request.challengeTag.trim(),
            description = request.description.trim(),
            inspirationImageUrls = normalizeImageUrls(request.inspirationImageUrls),
            startTime = request.startTime,
            deadline = request.deadline,
            status = normalizeChallengeStatus(request.status)
        )

        return ResponseEntity.ok(toAdminChallengeResponse(challengeRepository.save(updated)))
    }

    private fun toCurrentChallengeResponse(challenge: Challenge, userId: Long?): CurrentChallengeResponse {
        val now = LocalDateTime.now()
        val hasAttempted = userId?.let {
            challengeAttemptRepository.findByChallengeIdAndUserId(challenge.id, it) != null
        } ?: false
        val hasParticipated = userId?.let {
            challengeParticipantRepository.existsByChallengeIdAndUserId(challenge.id, it)
        } ?: false

        return CurrentChallengeResponse(
            challenge = toChallengeDto(challenge),
            isExpired = now.isAfter(challenge.deadline),
            participantCount = challengeParticipantRepository.countByChallengeId(challenge.id).toInt(),
            userStatus = when {
                hasParticipated -> "PARTICIPATED"
                hasAttempted -> "ATTEMPTED"
                else -> "NONE"
            }
        )
    }

    private fun toAdminChallengeResponse(challenge: Challenge): AdminChallengeResponse {
        return AdminChallengeResponse(
            challenge = toChallengeDto(challenge),
            isExpired = LocalDateTime.now().isAfter(challenge.deadline),
            participantCount = challengeParticipantRepository.countByChallengeId(challenge.id).toInt()
        )
    }

    private fun toChallengeDto(challenge: Challenge): ChallengeDto {
        return ChallengeDto(
            id = challenge.id,
            title = challenge.title,
            activityLabel = challenge.activityLabel,
            challengeTag = challenge.challengeTag,
            description = challenge.description,
            inspirationImageUrls = challenge.inspirationImageUrls,
            startTime = challenge.startTime,
            deadline = challenge.deadline,
            status = challenge.status
        )
    }

    private fun validateChallengeRequest(request: ChallengeRequest): String? {
        if (request.title.isBlank()) return "主题名不能为空"
        if (request.activityLabel.isBlank()) return "活动标签不能为空"
        if (request.challengeTag.isBlank()) return "社区挑战标签不能为空"
        if (request.deadline.isBefore(request.startTime)) return "截止时间不能早于开始时间"
        return null
    }

    private fun normalizeChallengeStatus(status: String): String {
        return when (status.uppercase()) {
            CHALLENGE_STATUS_PUBLISHED -> CHALLENGE_STATUS_PUBLISHED
            CHALLENGE_STATUS_ARCHIVED -> CHALLENGE_STATUS_ARCHIVED
            else -> CHALLENGE_STATUS_DRAFT
        }
    }

    private fun normalizeImageUrls(raw: String): String {
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
            .joinToString(",")
    }
}

data class ChallengeRequest(
    val title: String,
    val activityLabel: String,
    val challengeTag: String,
    val description: String = "",
    val inspirationImageUrls: String = "",
    val startTime: LocalDateTime,
    val deadline: LocalDateTime,
    val status: String = CHALLENGE_STATUS_DRAFT
)

data class ChallengeDto(
    val id: Long,
    val title: String,
    val activityLabel: String,
    val challengeTag: String,
    val description: String,
    val inspirationImageUrls: String,
    val startTime: LocalDateTime,
    val deadline: LocalDateTime,
    val status: String
)

data class CurrentChallengeResponse(
    val challenge: ChallengeDto?,
    val isExpired: Boolean = true,
    val participantCount: Int = 0,
    val userStatus: String = "NONE"
)

data class AdminChallengeResponse(
    val challenge: ChallengeDto,
    val isExpired: Boolean,
    val participantCount: Int
)
