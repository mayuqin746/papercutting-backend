package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Comment
import com.example.kpappercutting.repository.CommentRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    // 获取动态下的所有评论
    @GetMapping("/post/{postId}")
    fun getComments(@PathVariable postId: Long): List<Comment> =
        commentRepository.findByPostIdOrderByCreateTimeAsc(postId)

    // 发表评论
    @PostMapping("/create")
    fun createComment(@RequestBody body: Map<String, Any>): ResponseEntity<Any> {
        val postId = (body["postId"] as Number).toLong()
        val userId = (body["userId"] as Number).toLong()
        val content = body["content"] as String

        // 1. 获取 Post 和 User 对象（不再直接存 ID）
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.badRequest().body("帖子不存在")
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.badRequest().body("用户不存在")

        // 2. 构造评论对象并保存
        val comment = Comment(post = post, author = user, content = content)
        val saved = commentRepository.save(comment)

        // 3. 更新动态表中的评论计数（使用 copy 确保数据一致性）
        val updatedPost = post.copy(commentCount = post.commentCount + 1)
        postRepository.save(updatedPost)

        return ResponseEntity.ok(saved)
    }

    // 删除评论
    @DeleteMapping("/{commentId}")
    fun deleteComment(@PathVariable commentId: Long, @RequestParam userId: Long): ResponseEntity<Any> {
        val comment = commentRepository.findById(commentId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // 权限校验：只能删除自己的评论
        if (comment.author?.id != userId) return ResponseEntity.status(403).build()

        val post = comment.post // 获取该评论关联的动态
        commentRepository.delete(comment)

        // 评论数减1逻辑
        if (post != null) {
            val updatedPost = post.copy(commentCount = (post.commentCount - 1).coerceAtLeast(0))
            postRepository.save(updatedPost)
        }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}