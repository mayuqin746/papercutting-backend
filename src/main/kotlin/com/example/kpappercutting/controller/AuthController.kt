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
        // 1. 后端二次校验：长度和字符规则 (数字+英文)
        val regex = Regex("^[a-zA-Z0-9]*$")
        if (newUser.username.length < 4 || newUser.username.length > 16 || !newUser.username.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "账号格式不合规"))
        }
        if (newUser.password.length < 6 || newUser.password.length > 18 || !newUser.password.matches(regex)) {
            return ResponseEntity.status(400).body(mapOf("message" to "密码格式不合规"))
        }

        // 2. 检查用户名是否已存在
        if (userRepository.findByUsername(newUser.username) != null) {
            return ResponseEntity.status(400).body(mapOf("message" to "该账号已被注册"))
        }

        // 3. 初始化纯净数据
        val userToSave = newUser.copy(
            // 初始昵称设为“昵称”，这是你要求的默认展示文字
            nickname = if (newUser.nickname.isEmpty()) "昵称" else newUser.nickname,
            region = "", // 初始为空，让用户自己去编辑
            bio = "",    // 初始为空
            followerCount = 0,
            followingCount = 0,
            likedCount = 0
        )

        // 存入数据库
        val savedUser = userRepository.save(userToSave)
        return ResponseEntity.ok(savedUser)
    }

    //增加一个 update 接口，用来修改数据库里的资料。
    @PutMapping("/update")
    fun updateProfile(@RequestBody updatedUser: User): ResponseEntity<Any> {
        val existingUser = userRepository.findById(updatedUser.id).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "用户不存在"))

        // 更新允许修改的字段
        val savedUser = userRepository.save(existingUser.copy(
            nickname = updatedUser.nickname,
            bio = updatedUser.bio,
            region = updatedUser.region
        ))

        return ResponseEntity.ok(savedUser)
    }

    //个人页上传图片接口
    @PostMapping("/upload")
    fun uploadImage(
        @RequestParam("userId") userId: Long,
        @RequestParam("type") type: String,
        @RequestParam("image") file: MultipartFile
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null) ?: return ResponseEntity.notFound().build()

        try {
            // --- 优化：将路径设为电脑上的固定位置，不随项目变动 ---
            val uploadDirPath = "D:/kp_uploads"
            val uploadDir = File(uploadDirPath).apply { if (!exists()) mkdirs() }

            val fileName = "${UUID.randomUUID()}_${file.originalFilename}"
            val destFile = File(uploadDir, fileName)
            file.transferTo(destFile)

            val imageUrl = "http://10.21.170.92:8080/images/$fileName"

            val updatedUser = if (type == "avatar") user.copy(avatarUrl = imageUrl)
            else user.copy(backgroundUrl = imageUrl)

            return ResponseEntity.ok(userRepository.save(updatedUser))
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body(e.message)
        }
    }

//
}