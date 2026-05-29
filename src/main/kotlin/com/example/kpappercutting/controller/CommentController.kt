package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Comment
import com.example.kpappercutting.model.InteractionNotification
import com.example.kpappercutting.model.NOTIFICATION_TYPE_COMMENT
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.InteractionNotificationRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val notificationRepository: InteractionNotificationRepository,
    private val userRepository: UserRepository
) {
    // 获取动态下的所有评论
    @GetMapping("/post/{postId}")
    fun getComments(@PathVariable postId: Long): List<Comment> =
        commentRepository.findByPost_IdOrderByCreateTimeAsc(postId)

    // 发表评论
    @PostMapping("/create")
    fun createComment(@RequestBody body: Map<String, Any>): ResponseEntity<Any> {
        try {
            val postId = (body["postId"] as Number).toLong()
            val userId = (body["userId"] as Number).toLong()
            val content = body["content"] as String

            val post = postRepository.findById(postId).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "帖子不存在"))
            val user = userRepository.findById(userId).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "用户不存在"))

            val comment = Comment(post = post, author = user, content = content)
            val savedComment = commentRepository.save(comment)
            val recipient = post.author
            if (recipient != null && recipient.id != user.id) {
                notificationRepository.save(
                    InteractionNotification(
                        recipient = recipient,
                        actor = user,
                        post = post,
                        type = NOTIFICATION_TYPE_COMMENT,
                        commentId = savedComment.id,
                        commentContent = content
                    )
                )
            }

            // 更新评论数
            val updatedPost = post.copy(commentCount = post.commentCount + 1)
            postRepository.save(updatedPost)

            // 【修改点】：返回简单的成功状态，而不是返回整个 Comment 对象
            return ResponseEntity.ok(mapOf("status" to "success", "message" to "评论成功"))
        } catch (e: Exception) {
            return ResponseEntity.status(500).body(mapOf("error" to e.message))
        }
    }

    // 删除评论
    @DeleteMapping("/{commentId}")
    @Transactional
    fun deleteComment(@PathVariable commentId: Long, @RequestParam userId: Long): ResponseEntity<Any> {
        val comment = commentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // 权限校验：只能删除自己的评论
        if (comment.author?.id != userId) return ResponseEntity.status(403).build()

        val post = comment.post // 获取该评论关联的动态
        commentRepository.delete(comment)
        notificationRepository.deleteByCommentIdAndType(commentId, NOTIFICATION_TYPE_COMMENT)

        // 评论数减1逻辑
        if (post != null) {
            val updatedPost = post.copy(commentCount = (post.commentCount - 1).coerceAtLeast(0))
            postRepository.save(updatedPost)
        }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}
