//我们需要两个接口：一个获取所有动态，一个发布动态（目前你可以先手动在数据库塞数据，或者快速写个发布接口）。

package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.PostLike
import com.example.kpappercutting.repository.PostLikeRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*

@CrossOrigin // 重要：允许 Web 前端跨域访问

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository // 注入点赞仓库
) {

    @GetMapping("/all")
    fun getAllPosts(): List<Post> {
        // App 只拉取已通过(status=1)的内容
        return postRepository.findAllByStatusOrderByCreateTimeDesc(1)
    }

    // --- 新增：获取待审核列表 ---
    @GetMapping("/pending")
    fun getPendingPosts(): List<Post> {
        return postRepository.findAllByStatusOrderByCreateTimeDesc(0)
    }

    // --- 新增：审核操作接口 ---
    @PostMapping("/review")
    fun reviewPost(@RequestBody body: Map<String, Any>): ResponseEntity<Any> {
        val postId = (body["postId"] as? Number)?.toLong() ?: return ResponseEntity.badRequest().body("缺少ID")
        val action = body["action"] as? String // "pass" 或 "reject"

        val post = postRepository.findById(postId).orElse(null) ?: return ResponseEntity.notFound().build()

        if (action == "pass") {
            post.status = 1
        } else {
            post.status = 2 // 拒绝
        }

        postRepository.save(post)
        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    // 1. 动态图片上传接口
    @PostMapping("/upload")
    fun uploadPostImage(@RequestParam("image") file: MultipartFile): ResponseEntity<Any> {
        return try {
            val uploadDirPath = "D:/kp_uploads"
            val uploadDir = File(uploadDirPath).apply { if (!exists()) mkdirs() }
            val fileName = "${UUID.randomUUID()}.${file.originalFilename?.substringAfterLast(".", "jpg")}"
            val destFile = File(uploadDir, fileName)
            file.transferTo(destFile)

            //val imageUrl = "http://10.21.170.92:8080/images/$fileName"
            // 【修改为】：只保留相对路径，前头必须带 /
            val imageUrl = "/images/$fileName"
            ResponseEntity.ok(mapOf("url" to imageUrl))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(e.message)
        }
    }

    // 2. 创建动态接口
    @PostMapping("/create")
    fun createPost(@RequestBody postRequest: Map<String, Any>): ResponseEntity<Any> {
        val userId = (postRequest["userId"] as? Number)?.toLong() ?: return ResponseEntity.badRequest().body("缺少用户ID")
        val content = postRequest["content"] as? String ?: ""
        val imageUrl = postRequest["imageUrl"] as? String

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(404).body("用户不存在")

        val newPost = Post(
            author = user,
            content = content,
            imageUrl = imageUrl,
            createTime = java.time.LocalDateTime.now()
        )
        return ResponseEntity.ok(postRepository.save(newPost))
    }

    // 【新增】获取指定用户的动态列表
    @GetMapping("/user/{userId}")
    fun getUserPosts(@PathVariable userId: Long): List<Post> {
        return postRepository.findByAuthorIdOrderByCreateTimeDesc(userId)
    }

    // 3. 点赞/取消点赞 切换接口
    @PostMapping("/like")
    fun toggleLike(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val userId = body["userId"] ?: return ResponseEntity.badRequest().body("缺少userId")
        val postId = body["postId"] ?: return ResponseEntity.badRequest().body("缺少postId")

        val existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId)
        val post = postRepository.findById(postId).orElse(null) ?: return ResponseEntity.notFound().build()

        return if (existingLike != null) {
            postLikeRepository.delete(existingLike)
            val updatedPost = postRepository.save(post.copy(likeCount = (post.likeCount - 1).coerceAtLeast(0)))
            ResponseEntity.ok(mapOf("status" to "unliked", "count" to updatedPost.likeCount))
        } else {
            postLikeRepository.save(PostLike(userId = userId, postId = postId))
            val updatedPost = postRepository.save(post.copy(likeCount = post.likeCount + 1))
            ResponseEntity.ok(mapOf("status" to "liked", "count" to updatedPost.likeCount))
        }
    }

    // 4. 获取用户点赞过的作品列表
    @GetMapping("/liked/{userId}")
    fun getLikedPosts(@PathVariable userId: Long): List<Post> {
        val likedIds = postLikeRepository.findByUserId(userId).map { it.postId }
        return postRepository.findAllById(likedIds).reversed()
    }

    // 删除动态接口
    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long, @RequestParam userId: Long): ResponseEntity<Any> {
        val post = postRepository.findById(postId).orElse(null) ?: return ResponseEntity.notFound().build()

        // 权限校验：只有作者能删
        if (post.author?.id != userId) {
            return ResponseEntity.status(403).body("无权删除他人作品")
        }

        postRepository.delete(post)
        // 注意：实际开发中还需删除关联的点赞和评论，或者在数据库设置级联删除
        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}