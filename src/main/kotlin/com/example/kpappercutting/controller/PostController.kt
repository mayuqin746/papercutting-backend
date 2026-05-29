package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.PostLike
import com.example.kpappercutting.model.ChallengeParticipant
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.ChallengeAttemptRepository
import com.example.kpappercutting.repository.ChallengeParticipantRepository
import com.example.kpappercutting.repository.ChallengeRepository
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.PostLikeRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import jakarta.transaction.Transactional
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
    private val postLikeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository,
    private val challengeRepository: ChallengeRepository,
    private val challengeAttemptRepository: ChallengeAttemptRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository
) {

    @GetMapping("/all")
    fun getAllPosts(@RequestParam(required = false) viewerId: Long?): List<PostResponse> {
        return withLikedState(
            posts = postRepository.findAllByStatusOrderByCreateTimeDesc(1),
            viewerId = viewerId
        )
    }

    @GetMapping("/pending")
    fun getPendingPosts(): List<Post> {
        return postRepository.findAllByStatusOrderByCreateTimeDesc(0)
    }

    @GetMapping("/admin/review-list")
    fun getAdminReviewPosts(): List<PostResponse> {
        return postRepository.findAllByOrderByCreateTimeDesc().map { post ->
            toPostResponse(post, isLiked = false)
        }
    }

    @GetMapping("/admin/grouped-by-user")
    fun getAdminPostsGroupedByUser(): List<UserPostReviewGroup> {
        return postRepository.findAllByOrderByCreateTimeDesc()
            .groupBy { it.author?.id ?: 0L }
            .values
            .map { posts ->
                val author = posts.firstOrNull()?.author
                UserPostReviewGroup(
                    author = author,
                    totalCount = posts.size,
                    pendingCount = posts.count { it.status == 0 },
                    approvedCount = posts.count { it.status == 1 },
                    rejectedCount = posts.count { it.status == 2 },
                    posts = posts.map { toPostResponse(it, isLiked = false) }
                )
            }
            .sortedWith(
                compareByDescending<UserPostReviewGroup> { it.pendingCount }
                    .thenByDescending { it.posts.firstOrNull()?.createTime ?: LocalDateTime.MIN }
            )
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
    fun createPost(@RequestBody postRequest: Map<String, Any>): ResponseEntity<Any> {
        val userId = (postRequest["userId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body("缺少用户ID")

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
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): List<PostResponse> {
        return withLikedState(
            posts = postRepository.findByAuthorIdOrderByCreateTimeDesc(userId),
            viewerId = viewerId
        )
    }

    @PostMapping("/like")
    @Transactional
    fun toggleLike(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val userId = body["userId"]
            ?: return ResponseEntity.badRequest().body("缺少userId")

        val postId = body["postId"]
            ?: return ResponseEntity.badRequest().body("缺少postId")

        val existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId)
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return if (existingLike != null) {
            postLikeRepository.delete(existingLike)

            val updatedPost = postRepository.save(
                post.copy(likeCount = (post.likeCount - 1).coerceAtLeast(0))
            )
            post.author?.let { author ->
                userRepository.save(author.copy(likedCount = (author.likedCount - 1).coerceAtLeast(0)))
            }

            ResponseEntity.ok(mapOf("status" to "unliked", "count" to updatedPost.likeCount))
        } else {
            postLikeRepository.save(PostLike(userId = userId, postId = postId))

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
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): List<PostResponse> {
        val likedIds = postLikeRepository.findByUserId(userId).map { it.postId }
        return withLikedState(
            posts = postRepository.findAllById(likedIds).reversed(),
            viewerId = viewerId ?: userId
        )
    }

    // 删除动态接口
    @DeleteMapping("/{postId}")
    @Transactional
    fun deletePost(@PathVariable postId: Long, @RequestParam userId: Long): ResponseEntity<Any> {
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (post.author?.id != userId) {
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

private const val SHARE_TYPE_RESULT = "RESULT"
private const val SHARE_TYPE_DRAFT = "DRAFT"

data class UserPostReviewGroup(
    val author: User?,
    val totalCount: Int,
    val pendingCount: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val posts: List<PostResponse>
)
