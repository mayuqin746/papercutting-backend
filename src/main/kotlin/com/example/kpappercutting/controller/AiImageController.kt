package com.example.kpappercutting.controller

import com.example.kpappercutting.model.AiImageGenerationMode
import com.example.kpappercutting.service.AiImageGenerationException
import com.example.kpappercutting.service.AiImageGenerationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

@RestController
@RequestMapping("/api/ai/images")
class AiImageController(
    private val aiImageGenerationService: AiImageGenerationService
) {
    @PostMapping("/generate")
    fun generate(
        @RequestParam("prompt") prompt: String,
        @RequestParam("mode") mode: String,
        @RequestParam("count") count: Int,
        @RequestParam("referenceImage", required = false) referenceImage: MultipartFile?
    ): ResponseEntity<Any> {
        return try {
            val parsedMode = runCatching { AiImageGenerationMode.valueOf(mode) }
                .getOrElse { return ResponseEntity.badRequest().body(mapOf("message" to "mode 不合法")) }
            val imageDataUrl = referenceImage?.let(::toDataUrl)
            ResponseEntity.ok(
                aiImageGenerationService.generate(
                    prompt = prompt,
                    mode = parsedMode,
                    count = count,
                    imageDataUrl = imageDataUrl
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "请求参数不合法")))
        } catch (e: AiImageGenerationException) {
            ResponseEntity.status(e.status).body(mapOf("message" to (e.message ?: "AI 生图失败")))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("message" to (e.message ?: "AI 生图失败")))
        }
    }

    private fun toDataUrl(file: MultipartFile): String {
        if (file.isEmpty) throw IllegalArgumentException("参考图片不能为空")
        val contentType = file.contentType?.takeIf { it.startsWith("image/") }
            ?: throw IllegalArgumentException("只支持图片文件")
        val encoded = Base64.getEncoder().encodeToString(file.bytes)
        return "data:$contentType;base64,$encoded"
    }
}
