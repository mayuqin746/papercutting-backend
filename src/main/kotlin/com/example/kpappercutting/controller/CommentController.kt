package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Comment
import com.example.kpappercutting.model.CommentLike
import com.example.kpappercutting.model.InteractionNotification
import com.example.kpappercutting.model.NOTIFICATION_TYPE_COMMENT
import com.example.kpappercutting.model.NOTIFICATION_TYPE_COMMENT_LIKE
import com.example.kpappercutting.model.NOTIFICATION_TYPE_COMMENT_REPLY
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.CommentLikeRepository
import com.example.kpappercutting.repository.CommentReportRepository
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.InteractionNotificationRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import com.example.kpappercutting.security.currentUserIdOrNull
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val commentReportRepository: CommentReportRepository,
    private val postRepository: PostRepository,
    private val notificationRepository: InteractionNotificationRepository,
    private val userRepository: UserRepository
) {
    @GetMapping("/post/{postId}")
    fun getComments(
        request: HttpServletRequest,
        @PathVariable postId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): List<CommentResponse> {
        val comments = commentRepository.findByPost_IdOrderByCreateTimeAsc(postId)
        val currentUserId = request.currentUserIdOrNull() ?: viewerId
        val likedIds = currentUserId
            ?.let { userId -> commentLikeRepository.findByUserIdAndCommentIdIn(userId, comments.map { it.id }).map { it.commentId }.toSet() }
            .orEmpty()
        return comments.map { comment -> toCommentResponse(comment, comment.id in likedIds) }
    }

    @PostMapping("/create")
    @Transactional
    fun createComment(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        try {
            val postId = (body["postId"] as? Number)?.toLong()
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "缺少postId"))
            val userId = request.currentUserId()
            val content = (body["content"] as? String)?.trim()?.take(1000).orEmpty()
            val parentCommentId = (body["parentCommentId"] as? Number)?.toLong()
            val replyToCommentId = (body["replyToCommentId"] as? Number)?.toLong()

            if (content.isBlank()) {
                return ResponseEntity.badRequest().body(mapOf("error" to "评论内容不能为空"))
            }

            val post = postRepository.findById(postId).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "帖子不存在"))
            val user = userRepository.findById(userId).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "用户不存在"))
            val replyTarget = replyToCommentId
                ?.let { commentRepository.findById(it).orElse(null) }
            if (replyToCommentId != null && replyTarget == null) {
                return ResponseEntity.badRequest().body(mapOf("error" to "回复的评论不存在"))
            }

            val explicitParent = parentCommentId
                ?.let { commentRepository.findById(it).orElse(null) }
            if (parentCommentId != null && explicitParent == null) {
                return ResponseEntity.badRequest().body(mapOf("error" to "父评论不存在"))
            }

            val parent = when {
                replyTarget != null -> replyTarget.parentComment ?: replyTarget
                explicitParent != null -> explicitParent.parentComment ?: explicitParent
                else -> null
            }
            if (parent != null && parent.post?.id != postId) {
                return ResponseEntity.badRequest().body(mapOf("error" to "父评论不属于该动态"))
            }
            if (replyTarget != null && replyTarget.post?.id != postId) {
                return ResponseEntity.badRequest().body(mapOf("error" to "回复评论不属于该动态"))
            }
            val replyToUser = when {
                replyTarget?.author != null -> replyTarget.author
                parent?.author != null -> parent.author
                else -> null
            }

            val comment = Comment(
                post = post,
                author = user,
                parentComment = parent,
                replyToUser = if (parent == null) null else replyToUser,
                content = content
            )
            val savedComment = commentRepository.save(comment)

            val recipient = if (parent == null) post.author else replyToUser
            val notificationType = if (parent == null) NOTIFICATION_TYPE_COMMENT else NOTIFICATION_TYPE_COMMENT_REPLY
            if (recipient != null && recipient.id != user.id) {
                notificationRepository.save(
                    InteractionNotification(
                        recipient = recipient,
                        actor = user,
                        post = post,
                        type = notificationType,
                        commentId = savedComment.id,
                        commentContent = content
                    )
                )
            }

            val updatedPost = post.copy(commentCount = post.commentCount + 1)
            postRepository.save(updatedPost)

            return ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "评论成功",
                    "commentId" to savedComment.id
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.status(500).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/{commentId}/like")
    @Transactional
    fun toggleCommentLike(
        request: HttpServletRequest,
        @PathVariable commentId: Long
    ): ResponseEntity<Any> {
        val userId = request.currentUserId()
        val comment = commentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val post = comment.post ?: return ResponseEntity.badRequest().body(mapOf("message" to "评论缺少动态"))

        val existingLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
        return if (existingLike != null) {
            commentLikeRepository.delete(existingLike)
            val updated = commentRepository.save(comment.copy(likeCount = (comment.likeCount - 1).coerceAtLeast(0)))
            comment.author?.id?.let { recipientId ->
                notificationRepository.deleteByRecipientIdAndActorIdAndPostIdAndTypeAndCommentId(
                    recipientId = recipientId,
                    actorId = userId,
                    postId = post.id,
                    type = NOTIFICATION_TYPE_COMMENT_LIKE,
                    commentId = commentId
                )
            }
            ResponseEntity.ok(mapOf("status" to "unliked", "count" to updated.likeCount))
        } else {
            commentLikeRepository.save(CommentLike(userId = userId, commentId = commentId))
            val updated = commentRepository.save(comment.copy(likeCount = comment.likeCount + 1))
            val actor = userRepository.findById(userId).orElse(null)
            val recipient = comment.author
            if (actor != null && recipient != null && recipient.id != actor.id) {
                notificationRepository.deleteByRecipientIdAndActorIdAndPostIdAndTypeAndCommentId(
                    recipientId = recipient.id,
                    actorId = actor.id,
                    postId = post.id,
                    type = NOTIFICATION_TYPE_COMMENT_LIKE,
                    commentId = commentId
                )
                notificationRepository.save(
                    InteractionNotification(
                        recipient = recipient,
                        actor = actor,
                        post = post,
                        type = NOTIFICATION_TYPE_COMMENT_LIKE,
                        commentId = commentId,
                        commentContent = comment.content
                    )
                )
            }
            ResponseEntity.ok(mapOf("status" to "liked", "count" to updated.likeCount))
        }
    }

    @DeleteMapping("/{commentId}")
    @Transactional
    fun deleteComment(
        request: HttpServletRequest,
        @PathVariable commentId: Long,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val comment = commentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (comment.author?.id != authUserId) return ResponseEntity.status(403).build()

        deleteCommentCascade(comment)

        return ResponseEntity.ok(mapOf("status" to "success"))
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
        commentReportRepository.deleteByComment_IdIn(ids)
        notificationRepository.deleteByCommentIdIn(ids)

        commentsToDelete
            .sortedByDescending { if (it.parentComment == null) 0 else 1 }
            .forEach { commentRepository.delete(it) }

        if (post != null) {
            postRepository.save(post.copy(commentCount = (post.commentCount - ids.size).coerceAtLeast(0)))
        }
    }

    private fun toCommentResponse(comment: Comment, isLiked: Boolean): CommentResponse {
        return CommentResponse(
            id = comment.id,
            postId = comment.post?.id,
            parentId = comment.parentComment?.id,
            author = comment.author,
            replyToUser = comment.replyToUser,
            content = comment.content,
            likeCount = comment.likeCount,
            isLiked = isLiked,
            createTime = comment.createTime
        )
    }
}

data class CommentResponse(
    val id: Long,
    val postId: Long?,
    val parentId: Long?,
    val author: User?,
    val replyToUser: User?,
    val content: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val createTime: LocalDateTime
)
