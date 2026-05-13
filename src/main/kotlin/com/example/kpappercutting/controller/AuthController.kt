//后端接口，负责接手机传来的单子（登录/注册），去查底账。
package com.example.kpappercutting.controller

import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userRepository: UserRepository) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: Map<String, String>): ResponseEntity<Any> {
        val username = loginRequest["username"] ?: ""
        val password = loginRequest["password"] ?: ""

        val user = userRepository.findByUsername(username)

        return if (user != null && user.password == password) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.status(401).body(mapOf("message" to "用户名或密码错误"))
        }
    }

    // 注册接口
    @PostMapping("/register")
    fun register(@RequestBody newUser: User): ResponseEntity<Any> {
        val regex = Regex("^[a-zA-Z0-9]*$")

        if (newUser.username.length !in 4..16 || !newUser.username.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "账号格式不合规"))
        }

        if (newUser.password.length !in 6..18 || !newUser.password.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "密码格式不合规"))
        }

        if (userRepository.findByUsername(newUser.username) != null) {
            return ResponseEntity.status(400).body(mapOf("message" to "该账号已被注册"))
        }

        val userToSave = newUser.copy(
            nickname = newUser.nickname.ifEmpty { "昵称" },
            region = "", // 初始为空，让用户自己去编辑
            bio = "",    // 初始为空
            followerCount = 0,
            followingCount = 0,
            likedCount = 0
        )

        val savedUser = userRepository.save(userToSave)
        return ResponseEntity.ok(savedUser)
    }

    // 修改个人资料
    @PutMapping("/update")
    fun updateProfile(@RequestBody updatedUser: User): ResponseEntity<Any> {
        val existingUser = userRepository.findById(updatedUser.id).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))

        val savedUser = userRepository.save(
            existingUser.copy(
                nickname = updatedUser.nickname,
                bio = updatedUser.bio,
                region = updatedUser.region
            )
        )

        return ResponseEntity.ok(savedUser)
    }

    // 个人页上传图片接口：头像 / 背景图
    @PostMapping("/upload")
    fun uploadImage(
        @RequestParam("userId") userId: Long,
        @RequestParam("type") type: String,
        @RequestParam("image") file: MultipartFile
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return try {
            if (type != "avatar" && type != "background") {
                return ResponseEntity.badRequest().body(mapOf("message" to "type 只能是 avatar 或 background"))
            }

            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("message" to "上传文件不能为空"))
            }

            // 云服务器 Ubuntu 上的图片保存目录
            val uploadDirPath = "/home/ubuntu/kp_uploads"
            val uploadDir = File(uploadDirPath).apply {
                if (!exists()) mkdirs()
            }

            // 只保留后缀，避免原文件名包含特殊字符导致路径错误
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

            // 数据库只保存相对路径，不保存服务器 IP
            val imageUrl = "/images/$fileName"

            val updatedUser = if (type == "avatar") {
                user.copy(avatarUrl = imageUrl)
            } else {
                user.copy(backgroundUrl = imageUrl)
            }

            ResponseEntity.ok(userRepository.save(updatedUser))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(e.message)
        }
    }

    // 修改密码接口，先校验旧密码对不对，对了才允许改新密码。
    @PostMapping("/change-password")
    fun changePassword(@RequestBody request: Map<String, String>): ResponseEntity<Any> {
        val userId = request["userId"]?.toLong()
            ?: return ResponseEntity.badRequest().body("参数错误")

        val oldPassword = request["oldPassword"] ?: ""
        val newPassword = request["newPassword"] ?: ""

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (user.password != oldPassword) {
            return ResponseEntity.status(401).body(mapOf("message" to "原密码输入错误"))
        }

        val regex = Regex("^[a-zA-Z0-9]*$")
        if (newPassword.length < 6 || newPassword.length > 18 || !newPassword.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "新密码格式不合规（6-18位数字或字母）"))
        }

        val updatedUser = user.copy(password = newPassword)
        userRepository.save(updatedUser)

        return ResponseEntity.ok(mapOf("message" to "修改成功"))
    }
}