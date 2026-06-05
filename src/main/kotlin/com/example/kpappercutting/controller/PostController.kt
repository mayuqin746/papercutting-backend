package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.InteractionNotification
import com.example.kpappercutting.model.NOTIFICATION_TYPE_LIKE
import com.example.kpappercutting.model.PostLike
import com.example.kpappercutting.model.ChallengeParticipant
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.ChallengeAttemptRepository
import com.example.kpappercutting.repository.ChallengeParticipantRepository
import com.example.kpappercutting.repository.ChallengeRepository
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.InteractionNotificationRepository
import com.example.kpappercutting.repository.PostLikeRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.PostReportRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import com.example.kpappercutting.security.currentUserIdOrNull
import jakarta.servlet.http.HttpServletRequest
import jakarta.persistence.criteria.JoinType
import org.springframework.http.ResponseEntity
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: InteractionNotificationRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository,
    private val challengeRepository: ChallengeRepository,
    private val challengeAttemptRepository: ChallengeAttemptRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository,
    private val postReportRepository: PostReportRepository
) {

    @GetMapping("/all")
    fun getAllPosts(
        request: HttpServletRequest,
        @RequestParam(required = false) viewerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PostPageResponse {
        val pageable = postPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return toPostPageResponse(
            page = postRepository.findAllByStatus(1, pageable),
            viewerId = request.currentUserIdOrNull() ?: viewerId
        )
    }

    @GetMapping("/search")
    fun searchPosts(
        request: HttpServletRequest,
        @RequestParam keyword: String,
        @RequestParam(required = false) viewerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PostPageResponse {
        val safeKeyword = keyword.trim().take(120)
        val pageable = postPageRequest(
            page = page,
            size = size,
            sort = Sort.by(
                Sort.Order.desc("likeCount"),
                Sort.Order.desc("createTime")
            )
        )
        if (safeKeyword.isBlank()) return emptyPostPageResponse(pageable.pageNumber, pageable.pageSize)
        val searchTerms = buildPostSearchTerms(safeKeyword)
        val draftFilter = resolveDraftSearchFilter(safeKeyword)
        val spec = buildPostSearchSpecification(searchTerms, draftFilter)
        return toPostPageResponse(
            page = postRepository.findAll(spec, pageable),
            viewerId = request.currentUserIdOrNull() ?: viewerId
        )
    }

    private fun buildPostSearchTerms(keyword: String): List<String> {
        return keyword
            .replace("是否是草稿", " ")
            .replace("是不是草稿", " ")
            .replace("不是草稿", " ")
            .replace("非草稿", " ")
            .replace("草稿", " ")
            .replace("draft", " ", ignoreCase = true)
            .replace("普通作品", " ")
            .replace("普通动态", " ")
            .split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun resolveDraftSearchFilter(keyword: String): Boolean? {
        val lowerKeyword = keyword.lowercase()
        return when {
            "不是草稿" in lowerKeyword || "非草稿" in lowerKeyword ||
                    "普通作品" in lowerKeyword || "普通动态" in lowerKeyword -> false
            "草稿" in lowerKeyword || "draft" in lowerKeyword -> true
            else -> null
        }
    }

    private fun matchesPostSearch(
        post: Post,
        searchTerms: List<String>,
        draftFilter: Boolean?
    ): Boolean {
        if (draftFilter != null && (post.shareType == SHARE_TYPE_DRAFT) != draftFilter) {
            return false
        }
        if (searchTerms.isEmpty()) return draftFilter != null
        return searchTerms.all { term ->
            listOf(
                post.category,
                post.content,
                post.locationName,
                post.author?.nickname.orEmpty(),
                post.author?.username.orEmpty(),
                if (post.shareType == SHARE_TYPE_DRAFT) "草稿 draft 可编辑" else "普通 非草稿"
            ).any { value -> value.lowercase().contains(term) }
        }
    }

    @GetMapping("/pending")
    fun getPendingPosts(): List<Post> {
        return postRepository.findAllByStatusOrderByCreateTimeDesc(0)
    }

    @GetMapping("/admin/review-list")
    fun getAdminReviewPosts(
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<PostResponse> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return postRepository
            .findAll(buildAdminPostSpecification(resolveAdminPostStatus(status), keyword), pageable)
            .toAdminPageResponse { post -> toPostResponse(post, isLiked = false) }
    }

    @GetMapping("/admin/user-groups")
    fun getAdminPostUserGroups(
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<UserPostReviewGroup> {
        val pageable = adminPageRequest(page, size, Sort.unsorted())
        return postRepository
            .findAdminUserGroups(resolveAdminPostStatus(status), keyword.trim().take(120), pageable)
            .toAdminPageResponse { row ->
                UserPostReviewGroup(
                    author = row.author,
                    totalCount = row.totalCount.toInt(),
                    pendingCount = row.pendingCount.toInt(),
                    approvedCount = row.approvedCount.toInt(),
                    rejectedCount = row.rejectedCount.toInt(),
                    latestPostTime = row.latestPostTime
                )
            }
    }

    @GetMapping("/admin/user-groups/{userId}/posts")
    fun getAdminPostsForUserGroup(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "all") status: String,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<PostResponse> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return postRepository
            .findAll(buildAdminPostSpecification(resolveAdminPostStatus(status), keyword, userId), pageable)
            .toAdminPageResponse { post -> toPostResponse(post, isLiked = false) }
    }

    @PostMapping("/review")
    fun reviewPost(@RequestBody body: Map<String, Any>): ResponseEntity<Any> {
        val postId = (body["postId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body("缺少ID")

        val action = body["action"] as? String

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (action == "pass") {
            post.status = 1
        } else {
            post.status = 2 // 拒绝
        }

        val savedPost = postRepository.save(post)
        if (action == "pass") {
            registerChallengeParticipationIfEligible(savedPost)
        }
        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    // 动态图片上传接口
    @PostMapping("/upload")
    fun uploadPostImage(@RequestParam("image") file: MultipartFile): ResponseEntity<Any> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("message" to "上传文件不能为空"))
            }

            // 云服务器 Ubuntu 上的图片保存目录
            val uploadDirPath = "/home/ubuntu/kp_uploads"
            val uploadDir = File(uploadDirPath).apply {
                if (!exists()) mkdirs()
            }

            val extension = file.originalFilename
                ?.substringAfterLast(".", "jpg")
                ?.lowercase()
                ?: "jpg"

            val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
            if (extension !in allowedExtensions) {
                return ResponseEntity.badRequest().body(mapOf("message" to "只支持 jpg、jpeg、png、webp、gif 图片格式"))
            }

            val fileName = "${UUID.randomUUID()}.$extension"
            val destFile = File(uploadDir, fileName)

            file.transferTo(destFile)

            // 数据库和前端只使用相对路径
            val imageUrl = "/images/$fileName"

            ResponseEntity.ok(mapOf("url" to imageUrl))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(e.message)
        }
    }

    @PostMapping("/upload-draft")
    fun uploadPostDraft(@RequestParam("draft") file: MultipartFile): ResponseEntity<Any> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("message" to "上传草稿不能为空"))
            }

            val contentType = file.contentType.orEmpty().lowercase()
            val originalExtension = file.originalFilename
                ?.substringAfterLast(".", "zip")
                ?.lowercase()
                ?: "zip"
            if (originalExtension != "zip" && !contentType.contains("zip")) {
                return ResponseEntity.badRequest().body(mapOf("message" to "只支持 ZIP 草稿文件"))
            }

            val uploadDir = File("/home/ubuntu/kp_drafts").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${UUID.randomUUID()}.zip"
            val destFile = File(uploadDir, fileName)
            file.transferTo(destFile)

            ResponseEntity.ok(mapOf("url" to "/drafts/$fileName"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(e.message)
        }
    }

    // 创建动态接口
    @PostMapping("/create")
    fun createPost(
        request: HttpServletRequest,
        @RequestBody postRequest: Map<String, Any>
    ): ResponseEntity<Any> {
        val userId = request.currentUserId()
        val content = postRequest["content"] as? String ?: ""
        val category = sanitizeCategories(postRequest["category"] as? String)
        val imageUrl = postRequest["imageUrl"] as? String
        val imageUrls = sanitizeImageUrls(postRequest["imageUrls"] as? String, imageUrl)
        val showLocation = postRequest["showLocation"] as? Boolean ?: false
        val locationName = (postRequest["locationName"] as? String)?.trim()?.take(80) ?: ""
        val shareType = sanitizeShareType(postRequest["shareType"] as? String)
        val draftUrl = sanitizeDraftUrl(postRequest["draftUrl"] as? String)

        if (shareType == SHARE_TYPE_DRAFT && draftUrl == null) {
            return ResponseEntity.badRequest().body("草稿动态缺少草稿文件")
        }

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(404).body("用户不存在")

        val newPost = Post(
            author = user,
            content = content,
            category = category,
            imageUrl = imageUrl,
            imageUrls = imageUrls,
            showLocation = showLocation && locationName.isNotBlank(),
            locationName = if (showLocation) locationName else "",
            shareType = shareType,
            draftUrl = if (shareType == SHARE_TYPE_DRAFT) draftUrl else null,
            createTime = java.time.LocalDateTime.now()
        )

        return ResponseEntity.ok(toPostResponse(postRepository.save(newPost), userId))
    }

    private fun sanitizeCategories(rawCategory: String?): String {
        if (rawCategory.isNullOrBlank()) return ""

        val activeChallengeTags = challengeRepository
            .findByStatusAndDeadlineAfterOrderByStartTimeDescIdDesc(CHALLENGE_STATUS_PUBLISHED, LocalDateTime.now())
            .map { it.challengeTag.trim() }
            .filter { it.isNotEmpty() }
        val allowedCategories = (BASE_POST_CATEGORIES + activeChallengeTags).toSet()

        return rawCategory
            .split(",")
            .map { it.trim() }
            .filter { it in allowedCategories }
            .distinct()
            .take(10)
            .joinToString(",")
    }

    private fun sanitizeImageUrls(rawImageUrls: String?, fallbackImageUrl: String?): String {
        val urls = (rawImageUrls ?: fallbackImageUrl.orEmpty())
            .split(",")
            .map { it.trim() }
            .filter { it.startsWith("/images/") }
            .distinct()
            .take(9)

        return urls.joinToString(",")
    }

    private fun sanitizeShareType(rawShareType: String?): String {
        return if (rawShareType?.uppercase() == SHARE_TYPE_DRAFT) {
            SHARE_TYPE_DRAFT
        } else {
            SHARE_TYPE_RESULT
        }
    }

    private fun sanitizeDraftUrl(rawDraftUrl: String?): String? {
        val value = rawDraftUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!value.startsWith("/drafts/") || !value.endsWith(".zip")) return null
        val fileName = value.removePrefix("/drafts/")
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) return null
        return value
    }

    @GetMapping("/user/{userId}")
    fun getUserPosts(
        request: HttpServletRequest,
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PostPageResponse {
        val pageable = postPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return toPostPageResponse(
            page = postRepository.findByAuthorId(userId, pageable),
            viewerId = request.currentUserIdOrNull() ?: viewerId
        )
    }

    @PostMapping("/like")
    @Transactional
    fun toggleLike(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Long>
    ): ResponseEntity<Any> {
        val userId = request.currentUserId()
        val postId = body["postId"]
            ?: return ResponseEntity.badRequest().body("缺少postId")

        val existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId)
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return if (existingLike != null) {
            postLikeRepository.delete(existingLike)
            post.author?.id?.let { recipientId ->
                notificationRepository.deleteByRecipientIdAndActorIdAndPostIdAndType(
                    recipientId = recipientId,
                    actorId = userId,
                    postId = postId,
                    type = NOTIFICATION_TYPE_LIKE
                )
            }

            val updatedPost = postRepository.save(
                post.copy(likeCount = (post.likeCount - 1).coerceAtLeast(0))
            )
            post.author?.let { author ->
                userRepository.save(author.copy(likedCount = (author.likedCount - 1).coerceAtLeast(0)))
            }

            ResponseEntity.ok(mapOf("status" to "unliked", "count" to updatedPost.likeCount))
        } else {
            postLikeRepository.save(PostLike(userId = userId, postId = postId))
            val actor = userRepository.findById(userId).orElse(null)
            val recipient = post.author
            if (actor != null && recipient != null && recipient.id != actor.id) {
                notificationRepository.deleteByRecipientIdAndActorIdAndPostIdAndType(
                    recipientId = recipient.id,
                    actorId = actor.id,
                    postId = postId,
                    type = NOTIFICATION_TYPE_LIKE
                )
                notificationRepository.save(
                    InteractionNotification(
                        recipient = recipient,
                        actor = actor,
                        post = post,
                        type = NOTIFICATION_TYPE_LIKE
                    )
                )
            }

            val updatedPost = postRepository.save(
                post.copy(likeCount = post.likeCount + 1)
            )
            post.author?.let { author ->
                userRepository.save(author.copy(likedCount = author.likedCount + 1))
            }

            ResponseEntity.ok(mapOf("status" to "liked", "count" to updatedPost.likeCount))
        }
    }

    @GetMapping("/liked/{userId}")
    fun getLikedPosts(
        request: HttpServletRequest,
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PostPageResponse {
        val pageable = postPageRequest(page, size, Sort.unsorted())
        val likedPage = postLikeRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable)
        val likedIds = likedPage.content.map { it.postId }
        val postsById = postRepository.findAllById(likedIds).associateBy { it.id }
        val posts = likedIds.mapNotNull { postsById[it] }
        return toPostPageResponse(
            items = posts,
            page = likedPage.number,
            size = likedPage.size,
            totalElements = likedPage.totalElements,
            totalPages = likedPage.totalPages,
            viewerId = request.currentUserIdOrNull() ?: viewerId ?: userId
        )
    }

    // 删除动态接口
    @DeleteMapping("/{postId}")
    @Transactional
    fun deletePost(
        request: HttpServletRequest,
        @PathVariable postId: Long,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (post.author?.id != authUserId) {
            return ResponseEntity.status(403).body("无权删除他人作品")
        }

        val imageUrls = post.imageUrls.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOfNotNull(post.imageUrl) }
        val draftUrl = post.draftUrl

        commentRepository.deleteByPost_Id(postId)
        postLikeRepository.deleteByPostId(postId)
        notificationRepository.deleteByPostId(postId)
        postReportRepository.deleteByPost_Id(postId)
        challengeParticipantRepository.deleteByPostId(postId)
        postRepository.delete(post)

        // 尝试删除本地图片文件，失败不影响动态删除成功
        try {
            imageUrls.forEach { deleteLocalImage(it) }
            deleteLocalDraft(draftUrl)
        } catch (e: Exception) {
            println("删除动态资源失败：${e.message}")
        }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    private fun deleteLocalImage(imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return

        if (!imageUrl.startsWith("/images/")) return

        val fileName = imageUrl.removePrefix("/images/")
        if (fileName.contains("/") || fileName.contains("\\")) return

        val file = File("/home/ubuntu/kp_uploads", fileName)

        if (file.exists() && file.isFile) {
            file.delete()
        }
    }

    private fun deleteLocalDraft(draftUrl: String?) {
        val safeDraftUrl = sanitizeDraftUrl(draftUrl) ?: return
        val fileName = safeDraftUrl.removePrefix("/drafts/")
        val file = File("/home/ubuntu/kp_drafts", fileName)
        if (file.exists() && file.isFile) {
            file.delete()
        }
    }

    private fun withLikedState(posts: List<Post>, viewerId: Long?): List<PostResponse> {
        val likedPostIds = viewerId
            ?.let { postLikeRepository.findByUserId(it).map { like -> like.postId }.toSet() }
            .orEmpty()

        return posts.map { post ->
            toPostResponse(post, isLiked = post.id in likedPostIds)
        }
    }

    private fun toPostResponse(post: Post, viewerId: Long?): PostResponse {
        val isLiked = viewerId?.let { postLikeRepository.findByUserIdAndPostId(it, post.id) != null } ?: false
        return toPostResponse(post, isLiked)
    }

    private fun toPostResponse(post: Post, isLiked: Boolean): PostResponse {
        return PostResponse(
            id = post.id,
            author = post.author,
            content = post.content,
            category = post.category,
            imageUrl = post.imageUrl,
            imageUrls = post.imageUrls,
            showLocation = post.showLocation,
            locationName = post.locationName,
            shareType = post.shareType,
            draftUrl = post.draftUrl,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            createTime = post.createTime,
            status = post.status,
            isLiked = isLiked
        )
    }

    private fun toPostPageResponse(page: Page<Post>, viewerId: Long?): PostPageResponse {
        return toPostPageResponse(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            viewerId = viewerId
        )
    }

    private fun toPostPageResponse(
        items: List<Post>,
        page: Int,
        size: Int,
        totalElements: Long,
        totalPages: Int,
        viewerId: Long?
    ): PostPageResponse {
        return PostPageResponse(
            items = withLikedState(items, viewerId),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasMore = page + 1 < totalPages
        )
    }

    private fun emptyPostPageResponse(page: Int, size: Int): PostPageResponse {
        return PostPageResponse(
            items = emptyList(),
            page = page,
            size = size,
            totalElements = 0,
            totalPages = 0,
            hasMore = false
        )
    }

    private fun postPageRequest(page: Int, size: Int, sort: Sort): PageRequest {
        return PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(MIN_POST_PAGE_SIZE, MAX_POST_PAGE_SIZE),
            sort
        )
    }

    private fun buildPostSearchSpecification(
        searchTerms: List<String>,
        draftFilter: Boolean?
    ): Specification<Post> {
        return Specification { root, query, criteriaBuilder ->
            query.distinct(true)
            val author = root.join<Post, User>("author", JoinType.LEFT)
            val predicates = mutableListOf(
                criteriaBuilder.equal(root.get<Int>("status"), 1)
            )

            draftFilter?.let { isDraft ->
                predicates += if (isDraft) {
                    criteriaBuilder.equal(root.get<String>("shareType"), SHARE_TYPE_DRAFT)
                } else {
                    criteriaBuilder.notEqual(root.get<String>("shareType"), SHARE_TYPE_DRAFT)
                }
            }

            searchTerms.forEach { term ->
                val likeValue = "%${term.lowercase()}%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get<String>("category")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get<String>("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get<String>("locationName")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get<String>("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get<String>("username")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun resolveAdminPostStatus(status: String?): Int? {
        return when (status?.trim()?.lowercase()) {
            "pending" -> 0
            "approved" -> 1
            "rejected" -> 2
            else -> null
        }
    }

    private fun buildAdminPostSpecification(
        status: Int?,
        keyword: String,
        authorId: Long? = null
    ): Specification<Post> {
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val author = root.join<Post, User>("author", JoinType.LEFT)
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            status?.let {
                predicates += criteriaBuilder.equal(root.get<Int>("status"), it)
            }
            authorId?.let {
                predicates += criteriaBuilder.equal(author.get<Long>("id"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("locationName")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("username")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun registerChallengeParticipationIfEligible(post: Post) {
        val authorId = post.author?.id ?: return
        val categories = post.category
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (categories.isEmpty()) return

        val challenge = challengeRepository.findByStatusOrderByStartTimeDescIdDesc(CHALLENGE_STATUS_PUBLISHED)
            .firstOrNull { challenge ->
                challenge.challengeTag in categories &&
                        !post.createTime.isBefore(challenge.startTime) &&
                        !post.createTime.isAfter(challenge.deadline)
            } ?: return
        val attempt = challengeAttemptRepository.findByChallengeIdAndUserId(challenge.id, authorId) ?: return
        if (attempt.attemptTime.isAfter(post.createTime)) return
        if (challengeParticipantRepository.existsByChallengeIdAndUserId(challenge.id, authorId)) return

        challengeParticipantRepository.save(
            ChallengeParticipant(
                challengeId = challenge.id,
                userId = authorId,
                postId = post.id,
                participateTime = LocalDateTime.now()
            )
        )
    }
}

data class PostResponse(
    val id: Long,
    val author: User?,
    val content: String,
    val category: String,
    val imageUrl: String?,
    val imageUrls: String?,
    val showLocation: Boolean,
    val locationName: String,
    val shareType: String,
    val draftUrl: String?,
    val likeCount: Int,
    val commentCount: Int,
    val createTime: LocalDateTime,
    val status: Int,
    val isLiked: Boolean
)

data class PostPageResponse(
    val items: List<PostResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasMore: Boolean
)

private const val SHARE_TYPE_RESULT = "RESULT"
private const val SHARE_TYPE_DRAFT = "DRAFT"
private const val MIN_POST_PAGE_SIZE = 1
private const val MAX_POST_PAGE_SIZE = 50

data class UserPostReviewGroup(
    val author: User?,
    val totalCount: Int,
    val pendingCount: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val latestPostTime: LocalDateTime?
)
