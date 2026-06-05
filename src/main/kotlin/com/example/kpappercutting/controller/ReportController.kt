package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Comment
import com.example.kpappercutting.model.CommentReport
import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.PostReport
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.CommentLikeRepository
import com.example.kpappercutting.repository.CommentReportRepository
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.InteractionNotificationRepository
import com.example.kpappercutting.repository.PostReportRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.persistence.criteria.JoinType
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val postReportRepository: PostReportRepository,
    private val commentReportRepository: CommentReportRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val notificationRepository: InteractionNotificationRepository,
    private val userRepository: UserRepository
) {
    @PostMapping("/posts")
    fun createPostReport(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val postId = (body["postId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少postId"))
        val reporterId = request.currentUserId()
        val reason = (body["reason"] as? String)?.trim()?.take(80).orEmpty()
        val description = (body["description"] as? String)?.trim()?.take(500).orEmpty()

        if (reason.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "请选择举报原因"))
        }

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "动态不存在"))
        val reporter = userRepository.findById(reporterId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "举报用户不存在"))

        val report = postReportRepository.save(
            PostReport(
                post = post,
                reporter = reporter,
                reason = reason,
                description = description
            )
        )

        return ResponseEntity.ok(mapOf("status" to "success", "reportId" to report.id))
    }

    @PostMapping("/comments")
    fun createCommentReport(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val commentId = (body["commentId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少commentId"))
        val reporterId = request.currentUserId()
        val reason = (body["reason"] as? String)?.trim()?.take(80).orEmpty()
        val description = (body["description"] as? String)?.trim()?.take(500).orEmpty()

        if (reason.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "请选择举报原因"))
        }

        val comment = commentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "评论不存在"))
        val post = comment.post
            ?: return ResponseEntity.status(404).body(mapOf("message" to "评论所属动态不存在"))
        val reporter = userRepository.findById(reporterId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "举报用户不存在"))

        val report = commentReportRepository.save(
            CommentReport(
                comment = comment,
                post = post,
                reporter = reporter,
                reason = reason,
                description = description
            )
        )

        return ResponseEntity.ok(mapOf("status" to "success", "reportId" to report.id))
    }

    @GetMapping("/posts/admin")
    fun getPostReportsForAdmin(
        @RequestParam(required = false) reviewStatus: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<PostReportResponse> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return postReportRepository
            .findAll(buildPostReportSpecification(reviewStatus ?: status, keyword), pageable)
            .toAdminPageResponse(::toPostReportResponse)
    }

    @GetMapping("/comments/admin")
    fun getCommentReportsForAdmin(
        @RequestParam(required = false) reviewStatus: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): AdminPageResponse<CommentReportResponse> {
        val pageable = adminPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return commentReportRepository
            .findAll(buildCommentReportSpecification(reviewStatus ?: status, keyword), pageable)
            .toAdminPageResponse(::toCommentReportResponse)
    }

    @PostMapping("/posts/{reportId}/review")
    fun reviewPostReport(
        @PathVariable reportId: Long,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val reportResult = body["reportResult"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少举报结果"))
        val postAction = body["postAction"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少动态处理动作"))

        if (reportResult !in setOf("valid", "invalid")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "举报结果不合法"))
        }
        if (postAction !in setOf("keep", "reject", "approve")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "动态处理动作不合法"))
        }

        val report = postReportRepository.findById(reportId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        report.reviewStatus = "reviewed"
        report.reportResult = reportResult
        report.postAction = postAction
        report.reviewTime = LocalDateTime.now()

        report.post?.let { post ->
            when (postAction) {
                "reject" -> post.status = 2
                "approve" -> post.status = 1
            }
            postRepository.save(post)
        }

        postReportRepository.save(report)
        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    @PostMapping("/comments/{reportId}/review")
    @Transactional
    fun reviewCommentReport(
        @PathVariable reportId: Long,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val reportResult = body["reportResult"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少举报结果"))
        val commentAction = body["commentAction"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少评论处理动作"))

        if (reportResult !in setOf("valid", "invalid")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "举报结果不合法"))
        }
        if (commentAction !in setOf("keep", "delete")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "评论处理动作不合法"))
        }

        val report = commentReportRepository.findById(reportId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        report.reviewStatus = "reviewed"
        report.reportResult = reportResult
        report.commentAction = commentAction
        report.reviewTime = LocalDateTime.now()
        commentReportRepository.save(report)

        if (commentAction == "delete") {
            report.comment?.let(::deleteCommentCascade)
        }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    private fun toPostReportResponse(report: PostReport): PostReportResponse {
        return PostReportResponse(
            id = report.id,
            post = report.post,
            reporter = report.reporter,
            reason = report.reason,
            description = report.description,
            reviewStatus = report.reviewStatus,
            reportResult = report.reportResult,
            postAction = report.postAction,
            createTime = report.createTime,
            reviewTime = report.reviewTime
        )
    }

    private fun toCommentReportResponse(report: CommentReport): CommentReportResponse {
        return CommentReportResponse(
            id = report.id,
            comment = report.comment?.let(::toCommentSummaryResponse),
            post = report.post,
            reporter = report.reporter,
            reason = report.reason,
            description = report.description,
            reviewStatus = report.reviewStatus,
            reportResult = report.reportResult,
            commentAction = report.commentAction,
            createTime = report.createTime,
            reviewTime = report.reviewTime
        )
    }

    private fun toCommentSummaryResponse(comment: Comment): CommentSummaryResponse {
        return CommentSummaryResponse(
            id = comment.id,
            postId = comment.post?.id,
            parentId = comment.parentComment?.id,
            author = comment.author,
            replyToUser = comment.replyToUser,
            content = comment.content,
            likeCount = comment.likeCount,
            createTime = comment.createTime
        )
    }

    private fun deleteCommentCascade(comment: Comment) {
        val post = comment.post
        val commentsToDelete = if (comment.parentComment == null) {
            commentRepository.findByParentComment_IdOrderByCreateTimeAsc(comment.id) + comment
        } else {
            listOf(comment)
        }
        val ids = commentsToDelete.map { it.id }
        if (ids.isEmpty()) return

        commentLikeRepository.deleteByCommentIdIn(ids)
        notificationRepository.deleteByCommentIdIn(ids)
        commentReportRepository.detachComments(ids)

        commentsToDelete
            .sortedByDescending { if (it.parentComment == null) 0 else 1 }
            .forEach { commentRepository.delete(it) }

        if (post != null) {
            postRepository.save(post.copy(commentCount = (post.commentCount - ids.size).coerceAtLeast(0)))
        }
    }

    private fun buildPostReportSpecification(
        reviewStatus: String?,
        keyword: String
    ): Specification<PostReport> {
        val normalizedStatus = reviewStatus?.trim()?.lowercase()?.takeIf { it in setOf("pending", "reviewed") }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            val post = root.join<PostReport, Post>("post", JoinType.LEFT)
            val reporter = root.join<PostReport, User>("reporter", JoinType.LEFT)

            normalizedStatus?.let {
                predicates += criteriaBuilder.equal(root.get<String>("reviewStatus"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(post.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(post.get("category")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(reporter.get("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(reporter.get("username")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun buildCommentReportSpecification(
        reviewStatus: String?,
        keyword: String
    ): Specification<CommentReport> {
        val normalizedStatus = reviewStatus?.trim()?.lowercase()?.takeIf { it in setOf("pending", "reviewed") }
        val safeKeyword = keyword.trim().lowercase().take(120)
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            val comment = root.join<CommentReport, Comment>("comment", JoinType.LEFT)
            val post = root.join<CommentReport, Post>("post", JoinType.LEFT)
            val reporter = root.join<CommentReport, User>("reporter", JoinType.LEFT)
            val author = comment.join<Comment, User>("author", JoinType.LEFT)

            normalizedStatus?.let {
                predicates += criteriaBuilder.equal(root.get<String>("reviewStatus"), it)
            }
            if (safeKeyword.isNotBlank()) {
                val likeValue = "%$safeKeyword%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(comment.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(post.get("content")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(reporter.get("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(reporter.get("username")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("nickname")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(author.get("username")), likeValue)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}

data class PostReportResponse(
    val id: Long,
    val post: Post?,
    val reporter: User?,
    val reason: String,
    val description: String,
    val reviewStatus: String,
    val reportResult: String?,
    val postAction: String?,
    val createTime: LocalDateTime,
    val reviewTime: LocalDateTime?
)

data class CommentReportResponse(
    val id: Long,
    val comment: CommentSummaryResponse?,
    val post: Post?,
    val reporter: User?,
    val reason: String,
    val description: String,
    val reviewStatus: String,
    val reportResult: String?,
    val commentAction: String?,
    val createTime: LocalDateTime,
    val reviewTime: LocalDateTime?
)

data class CommentSummaryResponse(
    val id: Long,
    val postId: Long?,
    val parentId: Long?,
    val author: User?,
    val replyToUser: User?,
    val content: String,
    val likeCount: Int,
    val createTime: LocalDateTime
)
