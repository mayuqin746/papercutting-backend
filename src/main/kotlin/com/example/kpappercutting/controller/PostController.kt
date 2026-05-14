package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.PostLike
import com.example.kpappercutting.repository.PostLikeRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import jakarta.transaction.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository
) {

    @GetMapping("/all")
    fun getAllPosts(): List<Post> {
        return postRepository.findAllByStatusOrderByCreateTimeDesc(1)
    }

    @GetMapping("/pending")
    fun getPendingPosts(): List<Post> {
        return postRepository.findAllByStatusOrderByCreateTimeDesc(0)
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

        postRepository.save(post)
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

    // 创建动态接口
    @PostMapping("/create")
    fun createPost(@RequestBody postRequest: Map<String, Any>): ResponseEntity<Any> {
        val userId = (postRequest["userId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body("缺少用户ID")

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

    @GetMapping("/user/{userId}")
    fun getUserPosts(@PathVariable userId: Long): List<Post> {
        return postRepository.findByAuthorIdOrderByCreateTimeDesc(userId)
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
    fun getLikedPosts(@PathVariable userId: Long): List<Post> {
        val likedIds = postLikeRepository.findByUserId(userId).map { it.postId }
        return postRepository.findAllById(likedIds).reversed()
    }

    // 删除动态接口
    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long, @RequestParam userId: Long): ResponseEntity<Any> {
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (post.author?.id != userId) {
            return ResponseEntity.status(403).body("无权删除他人作品")
        }

        val imageUrl = post.imageUrl

        postRepository.delete(post)

        // 尝试删除本地图片文件，失败不影响动态删除成功
        try {
            deleteLocalImage(imageUrl)
        } catch (e: Exception) {
            println("删除动态图片失败：${e.message}")
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
}
