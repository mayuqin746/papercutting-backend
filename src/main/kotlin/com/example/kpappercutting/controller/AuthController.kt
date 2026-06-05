//后端接口，负责接手机传来的单子（登录/注册），去查底账。
package com.example.kpappercutting.controller

import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.JwtService
import com.example.kpappercutting.security.currentUserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        val username = loginRequest.username.trim()
        val password = loginRequest.password

        val user = userRepository.findByUsername(username)

        return if (user != null && verifyPassword(user, password)) {
            val securedUser = migrateLegacyPasswordIfNeeded(user, password)
            ResponseEntity.ok(securedUser.toAuthResponse())
        } else {
            ResponseEntity.status(401).body(mapOf("message" to "用户名或密码错误"))
        }
    }

    // 注册接口
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        val regex = Regex("^[a-zA-Z0-9]*$")
        val username = request.username.trim()
        val password = request.password

        if (username.length !in 4..16 || !username.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "账号格式不合规"))
        }

        if (password.length !in 6..18 || !password.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "密码格式不合规"))
        }

        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(400).body(mapOf("message" to "该账号已被注册"))
        }

        val userToSave = User(
            username = username,
            password = "",
            passwordHash = passwordEncoder.encode(password),
            nickname = request.nickname.ifEmpty { "昵称" },
            region = "", // 初始为空，让用户自己去编辑
            bio = "",    // 初始为空
            followerCount = 0,
            followingCount = 0,
            likedCount = 0
        )

        val savedUser = userRepository.save(userToSave)
        return ResponseEntity.ok(savedUser.toAuthResponse())
    }

    // 修改个人资料
    @PutMapping("/update")
    fun updateProfile(
        request: HttpServletRequest,
        @RequestBody updatedUser: User
    ): ResponseEntity<Any> {
        val existingUser = userRepository.findById(request.currentUserId()).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))

        val savedUser = userRepository.save(
            existingUser.copy(
                nickname = updatedUser.nickname,
                bio = updatedUser.bio,
                region = updatedUser.region
            )
        )

        return ResponseEntity.ok(savedUser.toSafeResponse())
    }

    // 个人页上传图片接口：头像 / 背景图
    @PostMapping("/upload")
    fun uploadImage(
        request: HttpServletRequest,
        @RequestParam("userId", required = false) userId: Long?,
        @RequestParam("type") type: String,
        @RequestParam("image") file: MultipartFile
    ): ResponseEntity<Any> {
        val authUserId = request.currentUserId()
        val user = userRepository.findById(authUserId).orElse(null)
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

            ResponseEntity.ok(userRepository.save(updatedUser).toSafeResponse())
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(e.message)
        }
    }

    // 修改密码接口，先校验旧密码对不对，对了才允许改新密码。
    @PostMapping("/change-password")
    fun changePassword(
        httpRequest: HttpServletRequest,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        val userId = httpRequest.currentUserId()
        val oldPassword = request["oldPassword"] ?: ""
        val newPassword = request["newPassword"] ?: ""

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (!verifyPassword(user, oldPassword)) {
            return ResponseEntity.status(401).body(mapOf("message" to "原密码输入错误"))
        }

        val regex = Regex("^[a-zA-Z0-9]*$")
        if (newPassword.length < 6 || newPassword.length > 18 || !newPassword.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "新密码格式不合规（6-18位数字或字母）"))
        }

        val updatedUser = user.copy(
            password = "",
            passwordHash = passwordEncoder.encode(newPassword)
        )
        userRepository.save(updatedUser)

        return ResponseEntity.ok(mapOf("message" to "修改成功"))
    }

    private fun verifyPassword(user: User, rawPassword: String): Boolean {
        if (user.passwordHash.isNotBlank()) {
            return passwordEncoder.matches(rawPassword, user.passwordHash)
        }
        return user.password.isNotBlank() && user.password == rawPassword
    }

    private fun migrateLegacyPasswordIfNeeded(user: User, rawPassword: String): User {
        if (user.passwordHash.isNotBlank()) return user
        if (user.password.isBlank()) return user
        return userRepository.save(
            user.copy(
                password = "",
                passwordHash = passwordEncoder.encode(rawPassword)
            )
        )
    }

    private fun User.toAuthResponse(): AuthResponse {
        return AuthResponse(
            token = jwtService.generateToken(id, username),
            user = toSafeResponse()
        )
    }

    private fun User.toSafeResponse(): UserResponse {
        return UserResponse(
            id = id,
            username = username,
            nickname = nickname,
            region = region,
            bio = bio,
            followingCount = followingCount,
            followerCount = followerCount,
            likedCount = likedCount,
            avatarUrl = avatarUrl,
            backgroundUrl = backgroundUrl
        )
    }
}

data class LoginRequest(
    val username: String = "",
    val password: String = ""
)

data class RegisterRequest(
    val username: String = "",
    val password: String = "",
    val nickname: String = ""
)

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val region: String = "",
    val bio: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likedCount: Int = 0,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null
)
