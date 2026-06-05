package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Knowledge
import com.example.kpappercutting.model.KnowledgeAnswerRecord
import com.example.kpappercutting.model.KnowledgeCollection
import com.example.kpappercutting.model.KnowledgeSubmission
import com.example.kpappercutting.model.User
import com.example.kpappercutting.model.UserReadRecord
import com.example.kpappercutting.repository.KnowledgeAnswerRecordRepository
import com.example.kpappercutting.repository.KnowledgeCollectionRepository
import com.example.kpappercutting.repository.KnowledgeRepository
import com.example.kpappercutting.repository.KnowledgeSubmissionRepository
import com.example.kpappercutting.repository.UserReadRecordRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.persistence.criteria.JoinType
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val KNOWLEDGE_STATUS_PUBLISHED = "PUBLISHED"
const val KNOWLEDGE_STATUS_ARCHIVED = "ARCHIVED"
const val KNOWLEDGE_SOURCE_OFFICIAL = "OFFICIAL"
const val KNOWLEDGE_SOURCE_USER = "USER"
const val KNOWLEDGE_SUBMISSION_PENDING = "PENDING"
const val KNOWLEDGE_SUBMISSION_ADOPTED = "ADOPTED"
const val KNOWLEDGE_SUBMISSION_REJECTED = "REJECTED"
const val KNOWLEDGE_DAILY_LIMIT = 5

val KNOWLEDGE_CATEGORIES = listOf("定义", "历史", "技艺", "信仰", "风格", "民俗", "符号", "民族", "传承", "现代")

@CrossOrigin
@RestController
@RequestMapping("/api/knowledge")
class KnowledgeController(
    private val knowledgeRepository: KnowledgeRepository,
    private val collectionRepository: KnowledgeCollectionRepository,
    private val readRecordRepository: UserReadRecordRepository,
    private val answerRecordRepository: KnowledgeAnswerRecordRepository,
    private val submissionRepository: KnowledgeSubmissionRepository,
    private val userRepository: UserRepository
) {
    private val appZone: ZoneId = ZoneId.of("Asia/Shanghai")

    @GetMapping("/home")
    fun getHomeKnowledge(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<KnowledgeHomeResponse> {
        val authUserId = request.currentUserId()
        val allKnowledge = knowledgeRepository.findByStatusOrderByIdAsc(KNOWLEDGE_STATUS_PUBLISHED)
        val userCollections = collectionRepository.findByUserIdOrderByCreateTimeAsc(authUserId)
        val collectedIds = userCollections.map { it.knowledgeId }.toSet()
        val readIds = readRecordRepository.findByUserId(authUserId).map { it.knowledgeId }.toSet()
        val answeredCorrectIds = allKnowledge
            .mapNotNull { knowledge -> answerRecordRepository.findByUserIdAndKnowledgeId(authUserId, knowledge.id) }
            .filter { it.isCorrect }
            .map { it.knowledgeId }
            .toSet()
        val todayReadCount = todayReadCount(authUserId).toInt()

        return ResponseEntity.ok(
            KnowledgeHomeResponse(
                totalCount = allKnowledge.size,
                readCount = userCollections.size,
                todayReadCount = todayReadCount,
                dailyLimit = KNOWLEDGE_DAILY_LIMIT,
                canOpenMore = todayReadCount < KNOWLEDGE_DAILY_LIMIT,
                categories = KNOWLEDGE_CATEGORIES,
                items = allKnowledge.map { knowledge ->
                    toKnowledgeDto(
                        knowledge = knowledge,
                        isOpened = knowledge.id in readIds,
                        isCollected = knowledge.id in collectedIds,
                        isAnsweredCorrect = knowledge.id in answeredCorrectIds
                    )
                }
            )
        )
    }

    @PostMapping("/open")
    fun openKnowledge(
        httpRequest: HttpServletRequest,
        @RequestBody request: KnowledgeOpenRequest
    ): ResponseEntity<Any> {
        val userId = httpRequest.currentUserId()
        val knowledgeId = request.knowledgeId
        if (!userRepository.existsById(userId)) return ResponseEntity.status(404).body("用户不存在")
        val knowledge = knowledgeRepository.findById(knowledgeId).orElse(null)
            ?: return ResponseEntity.status(404).body("科普知识不存在")
        if (knowledge.status != KNOWLEDGE_STATUS_PUBLISHED) return ResponseEntity.status(404).body("科普知识不存在")

        val existing = readRecordRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (existing == null) {
            val todayReadCount = todayReadCount(userId)
            if (todayReadCount >= KNOWLEDGE_DAILY_LIMIT) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    KnowledgeOpenResponse(
                        success = false,
                        message = "今日已完成5个科普打卡，明天再来",
                        todayReadCount = todayReadCount.toInt(),
                        dailyLimit = KNOWLEDGE_DAILY_LIMIT,
                        canOpenMore = false
                    )
                )
            }
            readRecordRepository.save(UserReadRecord(userId = userId, knowledgeId = knowledgeId, readAt = LocalDateTime.now(appZone)))
        }

        val updatedTodayReadCount = todayReadCount(userId).toInt()
        return ResponseEntity.ok(
            KnowledgeOpenResponse(
                success = true,
                message = "ok",
                todayReadCount = updatedTodayReadCount,
                dailyLimit = KNOWLEDGE_DAILY_LIMIT,
                canOpenMore = updatedTodayReadCount < KNOWLEDGE_DAILY_LIMIT
            )
        )
    }

    @PostMapping("/answer")
    fun answerKnowledge(
        httpRequest: HttpServletRequest,
        @RequestBody request: KnowledgeAnswerRequest
    ): ResponseEntity<KnowledgeAnswerResponse> {
        val userId = httpRequest.currentUserId()
        val knowledge = knowledgeRepository.findById(request.knowledgeId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                KnowledgeAnswerResponse(success = false, message = "科普知识不存在")
            )
        if (knowledge.status != KNOWLEDGE_STATUS_PUBLISHED) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                KnowledgeAnswerResponse(success = false, message = "科普知识不存在")
            )
        }
        if (readRecordRepository.findByUserIdAndKnowledgeId(userId, request.knowledgeId) == null) {
            return ResponseEntity.badRequest().body(
                KnowledgeAnswerResponse(success = false, message = "请先阅读该科普知识")
            )
        }
        if (request.answer.isBlank()) {
            return ResponseEntity.badRequest().body(
                KnowledgeAnswerResponse(success = false, message = "请选择答案")
            )
        }

        val isCorrect = normalizeAnswer(request.answer) == normalizeAnswer(knowledge.answer)
        val existing = answerRecordRepository.findByUserIdAndKnowledgeId(userId, request.knowledgeId)
        val record = existing?.copy(
            selectedAnswer = request.answer,
            isCorrect = isCorrect,
            answeredAt = LocalDateTime.now(appZone)
        ) ?: KnowledgeAnswerRecord(
            userId = userId,
            knowledgeId = request.knowledgeId,
            selectedAnswer = request.answer,
            isCorrect = isCorrect,
            answeredAt = LocalDateTime.now(appZone)
        )
        answerRecordRepository.save(record)

        return ResponseEntity.ok(
            KnowledgeAnswerResponse(
                success = true,
                message = if (isCorrect) "回答正确" else "回答错误",
                correct = isCorrect,
                explanation = knowledge.answerExplanation,
                eligibleToCollect = isCorrect
            )
        )
    }

    @PostMapping("/collect")
    fun collectKnowledge(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Long>
    ): ResponseEntity<Any> {
        val userId = request.currentUserId()
        val knowledgeId = body["knowledgeId"] ?: return ResponseEntity.badRequest().body("missing knowledgeId")
        val answerRecord = answerRecordRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (answerRecord?.isCorrect != true) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("答对题目后才能收集")
        }

        val existing = collectionRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (existing == null) {
            collectionRepository.save(KnowledgeCollection(userId = userId, knowledgeId = knowledgeId))
        }
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @GetMapping("/collections/{userId}")
    fun getCollections(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<KnowledgeCollectionPageResponse> {
        val authUserId = request.currentUserId()
        val collections = collectionRepository.findByUserIdOrderByCreateTimeAsc(authUserId)
        val collectionMap = collections.associateBy { it.knowledgeId }
        val collectedData = knowledgeRepository.findAllById(collectionMap.keys)
            .sortedBy { collectionMap[it.id]?.createTime }
            .map { knowledge ->
                toKnowledgeDto(
                    knowledge = knowledge,
                    isOpened = true,
                    isCollected = true,
                    isAnsweredCorrect = true
                )
            }

        val submissions = submissionRepository.findByUserIdOrderByCreateTimeDesc(authUserId).map(::toSubmissionDto)
        return ResponseEntity.ok(KnowledgeCollectionPageResponse(collections = collectedData, submissions = submissions))
    }

    @PostMapping("/submissions")
    fun createSubmission(
        httpRequest: HttpServletRequest,
        @RequestBody request: KnowledgeSubmissionRequest
    ): ResponseEntity<Any> {
        val userId = httpRequest.currentUserId()
        if (!userRepository.existsById(userId)) return ResponseEntity.status(404).body("用户不存在")
        val validationError = validateSubmission(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val submission = KnowledgeSubmission(
            userId = userId,
            category = normalizeCategory(request.category),
            title = request.title.trim(),
            content = request.content.trim(),
            questionType = normalizeQuestionType(request.questionType),
            questionText = request.questionText.trim(),
            optionsJson = request.optionsJson.trim(),
            answer = request.answer.trim(),
            answerExplanation = request.answerExplanation.trim(),
            imageUrls = normalizeImageUrls(request.imageUrls)
        )

        return ResponseEntity.ok(toSubmissionDto(submissionRepository.save(submission)))
    }

    @GetMapping("/submissions/{userId}")
    fun getUserSubmissions(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): List<KnowledgeSubmissionDto> {
        return submissionRepository.findByUserIdOrderByCreateTimeDesc(request.currentUserId()).map(::toSubmissionDto)
    }

    @GetMapping("/admin/submissions")
    fun getAdminSubmissions(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "全部") category: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<KnowledgeSubmissionDto> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return submissionRepository
            .findAll(buildKnowledgeSubmissionSpecification(status, category, keyword), pageable)
            .toAdminPageResponse(::toSubmissionDto)
    }

    @GetMapping("/admin")
    fun getAdminKnowledge(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "全部") category: String,
        @RequestParam(defaultValue = "all") sourceType: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<AdminKnowledgeDto> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.ASC, "id"))
        return knowledgeRepository
            .findAll(buildKnowledgeSpecification(status, category, sourceType, keyword), pageable)
            .toAdminPageResponse(::toAdminKnowledgeDto)
    }

    @PostMapping("/admin")
    fun createAdminKnowledge(@RequestBody request: AdminKnowledgeRequest): ResponseEntity<Any> {
        val validationError = validateAdminKnowledge(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val knowledge = Knowledge(
            category = normalizeCategory(request.category),
            title = request.title.trim(),
            content = request.content.trim(),
            questionType = normalizeQuestionType(request.questionType),
            questionText = request.questionText.trim(),
            optionsJson = normalizeOptionsJson(request.optionsJson, request.questionType),
            answer = request.answer.trim(),
            answerExplanation = request.answerExplanation.trim(),
            sourceType = KNOWLEDGE_SOURCE_OFFICIAL,
            status = normalizeKnowledgeStatus(request.status)
        )

        return ResponseEntity.ok(toAdminKnowledgeDto(knowledgeRepository.save(knowledge)))
    }

    @PutMapping("/admin/{knowledgeId}")
    fun updateAdminKnowledge(
        @PathVariable knowledgeId: Long,
        @RequestBody request: AdminKnowledgeRequest
    ): ResponseEntity<Any> {
        val existing = knowledgeRepository.findById(knowledgeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val validationError = validateAdminKnowledge(request)
        if (validationError != null) return ResponseEntity.badRequest().body(validationError)

        val updated = existing.copy(
            category = normalizeCategory(request.category),
            title = request.title.trim(),
            content = request.content.trim(),
            questionType = normalizeQuestionType(request.questionType),
            questionText = request.questionText.trim(),
            optionsJson = normalizeOptionsJson(request.optionsJson, request.questionType),
            answer = request.answer.trim(),
            answerExplanation = request.answerExplanation.trim(),
            status = normalizeKnowledgeStatus(request.status)
        )

        return ResponseEntity.ok(toAdminKnowledgeDto(knowledgeRepository.save(updated)))
    }

    @PostMapping("/admin/{knowledgeId}/status")
    fun updateAdminKnowledgeStatus(
        @PathVariable knowledgeId: Long,
        @RequestBody request: KnowledgeStatusRequest
    ): ResponseEntity<Any> {
        val existing = knowledgeRepository.findById(knowledgeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val updated = existing.copy(status = normalizeKnowledgeStatus(request.status))
        return ResponseEntity.ok(toAdminKnowledgeDto(knowledgeRepository.save(updated)))
    }

    @DeleteMapping("/admin/{knowledgeId}")
    fun deleteAdminKnowledge(@PathVariable knowledgeId: Long): ResponseEntity<Any> {
        if (!knowledgeRepository.existsById(knowledgeId)) return ResponseEntity.notFound().build()
        knowledgeRepository.deleteById(knowledgeId)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/admin/submissions/{submissionId}/review")
    @Transactional
    fun reviewSubmission(
        @PathVariable submissionId: Long,
        @RequestBody request: KnowledgeSubmissionReviewRequest
    ): ResponseEntity<Any> {
        val submission = submissionRepository.findById(submissionId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val action = request.action.lowercase()

        if (action == "adopt") {
            val associatedKnowledge = knowledgeRepository.findAllByAuthorSubmissionId(submission.id)
            val existingKnowledge = associatedKnowledge.firstOrNull()
            val knowledge = knowledgeRepository.save(
                existingKnowledge?.copy(
                    category = submission.category,
                    title = submission.title,
                    content = submission.content,
                    questionType = submission.questionType,
                    questionText = submission.questionText,
                    optionsJson = submission.optionsJson,
                    answer = submission.answer,
                    answerExplanation = submission.answerExplanation,
                    sourceType = KNOWLEDGE_SOURCE_USER,
                    status = KNOWLEDGE_STATUS_PUBLISHED,
                    authorSubmissionId = submission.id
                ) ?: Knowledge(
                    category = submission.category,
                    title = submission.title,
                    content = submission.content,
                    questionType = submission.questionType,
                    questionText = submission.questionText,
                    optionsJson = submission.optionsJson,
                    answer = submission.answer,
                    answerExplanation = submission.answerExplanation,
                    sourceType = KNOWLEDGE_SOURCE_USER,
                    status = KNOWLEDGE_STATUS_PUBLISHED,
                    authorSubmissionId = submission.id
                )
            )
            associatedKnowledge.drop(1).forEach { duplicatedKnowledge ->
                knowledgeRepository.save(duplicatedKnowledge.copy(status = KNOWLEDGE_STATUS_ARCHIVED))
            }
            submission.status = KNOWLEDGE_SUBMISSION_ADOPTED
            submission.reviewNote = request.reviewNote.orEmpty()
            submission.reviewTime = LocalDateTime.now(appZone)
            submissionRepository.save(submission)
            return ResponseEntity.ok(mapOf("success" to true, "knowledgeId" to knowledge.id))
        }

        if (action == "reject") {
            knowledgeRepository.findAllByAuthorSubmissionId(submission.id).forEach { knowledge ->
                knowledgeRepository.save(knowledge.copy(status = KNOWLEDGE_STATUS_ARCHIVED))
            }
            submission.status = KNOWLEDGE_SUBMISSION_REJECTED
            submission.reviewNote = request.reviewNote.orEmpty()
            submission.reviewTime = LocalDateTime.now(appZone)
            submissionRepository.save(submission)
            return ResponseEntity.ok(mapOf("success" to true))
        }

        return ResponseEntity.badRequest().body("未知审核动作")
    }

    private fun todayReadCount(userId: Long): Long {
        val today = LocalDate.now(appZone)
        val start = today.atStartOfDay()
        val end = LocalDateTime.of(today, LocalTime.MAX)
        return readRecordRepository.countByUserIdAndReadAtBetween(userId, start, end)
    }

    private fun toKnowledgeDto(
        knowledge: Knowledge,
        isOpened: Boolean,
        isCollected: Boolean,
        isAnsweredCorrect: Boolean
    ): KnowledgeDto {
        return KnowledgeDto(
            id = knowledge.id,
            category = knowledge.category,
            title = knowledge.title,
            content = knowledge.content,
            questionType = knowledge.questionType,
            questionText = knowledge.questionText,
            optionsJson = knowledge.optionsJson,
            answer = knowledge.answer,
            answerExplanation = knowledge.answerExplanation,
            isRead = isCollected,
            isOpened = isOpened,
            isCollected = isCollected,
            isAnsweredCorrect = isAnsweredCorrect
        )
    }

    private fun toSubmissionDto(submission: KnowledgeSubmission): KnowledgeSubmissionDto {
        return KnowledgeSubmissionDto(
            id = submission.id,
            userId = submission.userId,
            author = submission.author,
            category = submission.category,
            title = submission.title,
            content = submission.content,
            questionType = submission.questionType,
            questionText = submission.questionText,
            optionsJson = submission.optionsJson,
            answer = submission.answer,
            answerExplanation = submission.answerExplanation,
            imageUrls = submission.imageUrls,
            status = submission.status,
            reviewNote = submission.reviewNote,
            createTime = submission.createTime,
            reviewTime = submission.reviewTime
        )
    }

    private fun toAdminKnowledgeDto(knowledge: Knowledge): AdminKnowledgeDto {
        return AdminKnowledgeDto(
            id = knowledge.id,
            category = knowledge.category,
            title = knowledge.title,
            content = knowledge.content,
            questionType = knowledge.questionType,
            questionText = knowledge.questionText,
            optionsJson = knowledge.optionsJson,
            answer = knowledge.answer,
            answerExplanation = knowledge.answerExplanation,
            sourceType = knowledge.sourceType,
            status = knowledge.status,
            authorSubmissionId = knowledge.authorSubmissionId,
            createTime = knowledge.createTime
        )
    }

    private fun validateAdminKnowledge(request: AdminKnowledgeRequest): String? {
        if (request.title.isBlank()) return "标题不能为空"
        if (request.content.isBlank()) return "内容不能为空"
        if (request.questionText.isBlank()) return "题目不能为空"
        if (request.answer.isBlank()) return "答案不能为空"
        return null
    }

    private fun validateSubmission(request: KnowledgeSubmissionRequest): String? {
        if (request.title.isBlank()) return "标题不能为空"
        if (request.content.isBlank()) return "内容不能为空"
        if (request.questionText.isBlank()) return "题目不能为空"
        if (request.answer.isBlank()) return "答案不能为空"
        return null
    }

    private fun normalizeCategory(category: String): String {
        return category.trim().takeIf { it in KNOWLEDGE_CATEGORIES } ?: KNOWLEDGE_CATEGORIES.first()
    }

    private fun normalizeQuestionType(type: String): String {
        return when (type.uppercase()) {
            "SINGLE_CHOICE" -> "SINGLE_CHOICE"
            else -> "TRUE_FALSE"
        }
    }

    private fun normalizeKnowledgeStatus(status: String): String {
        return when (status.uppercase()) {
            KNOWLEDGE_STATUS_ARCHIVED -> KNOWLEDGE_STATUS_ARCHIVED
            else -> KNOWLEDGE_STATUS_PUBLISHED
        }
    }

    private fun normalizeOptionsJson(rawOptionsJson: String, questionType: String): String {
        if (normalizeQuestionType(questionType) == "TRUE_FALSE") return "[\"正确\",\"错误\"]"
        return rawOptionsJson.trim()
    }

    private fun normalizeImageUrls(rawImageUrls: String): String {
        return rawImageUrls.split(",")
            .map { it.trim() }
            .filter { it.startsWith("/images/") }
            .distinct()
            .take(9)
            .joinToString(",")
    }

    private fun normalizeAnswer(answer: String): String {
        return when (answer.trim().lowercase()) {
            "false", "错", "错误", "✗", "x", "否", "0" -> "false"
            "true", "对", "正确", "✓", "√", "是", "1" -> "true"
            else -> answer.trim().lowercase()
        }
    }

    private fun buildKnowledgeSubmissionSpecification(
        status: String?,
        category: String,
        keyword: String
    ): Specification<KnowledgeSubmission> {
        val normalizedStatus = status?.uppercase()?.takeIf {
            it in setOf(KNOWLEDGE_SUBMISSION_PENDING, KNOWLEDGE_SUBMISSION_ADOPTED, KNOWLEDGE_SUBMISSION_REJECTED)
        }
        val normalizedCategory = category.trim().takeIf { it in KNOWLEDGE_CATEGORIES }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            val author = root.join<KnowledgeSubmission, User>("author", JoinType.LEFT)

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
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("questionText")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("username")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun buildKnowledgeSpecification(
        status: String?,
        category: String,
        sourceType: String,
        keyword: String
    ): Specification<Knowledge> {
        val normalizedStatus = status?.uppercase()?.takeIf { it in setOf(KNOWLEDGE_STATUS_PUBLISHED, KNOWLEDGE_STATUS_ARCHIVED) }
        val normalizedCategory = category.trim().takeIf { it in KNOWLEDGE_CATEGORIES }
        val normalizedSourceType = sourceType.trim().uppercase().takeIf { it in setOf(KNOWLEDGE_SOURCE_OFFICIAL, KNOWLEDGE_SOURCE_USER) }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            normalizedStatus?.let {
                predicates += criteriaBuilder.equal(root.get<String>("status"), it)
            }
            normalizedCategory?.let {
                predicates += criteriaBuilder.equal(root.get<String>("category"), it)
            }
            normalizedSourceType?.let {
                predicates += criteriaBuilder.equal(root.get<String>("sourceType"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("questionText")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}

data class KnowledgeDto(
    val id: Long,
    val category: String,
    val title: String,
    val content: String,
    val questionType: String,
    val questionText: String,
    val optionsJson: String,
    val answer: String,
    val answerExplanation: String,
    val isRead: Boolean,
    val isOpened: Boolean,
    val isCollected: Boolean,
    val isAnsweredCorrect: Boolean
)

data class KnowledgeHomeResponse(
    val totalCount: Int,
    val readCount: Int,
    val todayReadCount: Int,
    val dailyLimit: Int,
    val canOpenMore: Boolean,
    val categories: List<String>,
    val items: List<KnowledgeDto>
)

data class KnowledgeCollectionPageResponse(
    val collections: List<KnowledgeDto>,
    val submissions: List<KnowledgeSubmissionDto>
)

data class KnowledgeOpenRequest(
    val userId: Long,
    val knowledgeId: Long
)

data class KnowledgeOpenResponse(
    val success: Boolean,
    val message: String,
    val todayReadCount: Int,
    val dailyLimit: Int,
    val canOpenMore: Boolean
)

data class KnowledgeAnswerRequest(
    val userId: Long,
    val knowledgeId: Long,
    val answer: String
)

data class KnowledgeAnswerResponse(
    val success: Boolean = true,
    val message: String = "",
    val correct: Boolean = false,
    val explanation: String = "",
    val eligibleToCollect: Boolean = false
)

data class AdminKnowledgeRequest(
    val category: String,
    val title: String,
    val content: String,
    val questionType: String = "TRUE_FALSE",
    val questionText: String,
    val optionsJson: String = "",
    val answer: String,
    val answerExplanation: String,
    val status: String = KNOWLEDGE_STATUS_PUBLISHED
)

data class KnowledgeStatusRequest(
    val status: String
)

data class AdminKnowledgeDto(
    val id: Long,
    val category: String,
    val title: String,
    val content: String,
    val questionType: String,
    val questionText: String,
    val optionsJson: String,
    val answer: String,
    val answerExplanation: String,
    val sourceType: String,
    val status: String,
    val authorSubmissionId: Long?,
    val createTime: LocalDateTime
)

data class KnowledgeSubmissionRequest(
    val userId: Long?,
    val category: String,
    val title: String,
    val content: String,
    val questionType: String = "TRUE_FALSE",
    val questionText: String,
    val optionsJson: String = "",
    val answer: String,
    val answerExplanation: String,
    val imageUrls: String = ""
)

data class KnowledgeSubmissionReviewRequest(
    val action: String,
    val reviewNote: String? = null
)

data class KnowledgeSubmissionDto(
    val id: Long,
    val userId: Long,
    val author: User?,
    val category: String,
    val title: String,
    val content: String,
    val questionType: String,
    val questionText: String,
    val optionsJson: String,
    val answer: String,
    val answerExplanation: String,
    val imageUrls: String,
    val status: String,
    val reviewNote: String,
    val createTime: LocalDateTime,
    val reviewTime: LocalDateTime?
)
