package com.example.kpappercutting.service

import com.example.kpappercutting.model.AiGeneratedImageResponse
import com.example.kpappercutting.model.AiImageGenerationMode
import com.example.kpappercutting.model.AiImageGenerationResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

data class SeedreamImageRequest(
    val prompt: String,
    val imageDataUrl: String? = null
)

interface AiImageGateway {
    fun generateOne(request: SeedreamImageRequest): String
}

class AiImageGenerationException(
    message: String,
    val status: HttpStatus = HttpStatus.BAD_GATEWAY
) : RuntimeException(message)

@Service
class AiImageGenerationService(
    private val gateway: AiImageGateway
) {
    fun generate(
        prompt: String,
        mode: AiImageGenerationMode,
        count: Int,
        imageDataUrl: String?
    ): AiImageGenerationResponse {
        val trimmedPrompt = prompt.trim()
        require(trimmedPrompt.isNotBlank()) { "请输入构思描述" }

        val resolvedCount = expectedCountFor(mode)
        require(count == resolvedCount) {
            if (mode == AiImageGenerationMode.TEXT_TO_IMAGE) {
                "文生图一次必须生成 2 张"
            } else {
                "图生图一次必须生成 1 张"
            }
        }
        if (mode == AiImageGenerationMode.IMAGE_TO_IMAGE) {
            require(!imageDataUrl.isNullOrBlank()) { "图生图需要上传参考图片" }
        }

        val finalPrompt = buildSeedreamPrompt(trimmedPrompt)
        val images = (1..resolvedCount).map {
            val b64Json = gateway.generateOne(
                SeedreamImageRequest(
                    prompt = finalPrompt,
                    imageDataUrl = imageDataUrl.takeIf { mode == AiImageGenerationMode.IMAGE_TO_IMAGE }
                )
            )
            if (b64Json.isBlank()) {
                throw AiImageGenerationException("AI 服务没有返回图片数据")
            }
            AiGeneratedImageResponse(b64Json = b64Json)
        }

        return AiImageGenerationResponse(
            mode = mode,
            images = images
        )
    }

    private fun expectedCountFor(mode: AiImageGenerationMode): Int {
        return when (mode) {
            AiImageGenerationMode.TEXT_TO_IMAGE -> 2
            AiImageGenerationMode.IMAGE_TO_IMAGE -> 1
        }
    }

    private fun buildSeedreamPrompt(userPrompt: String): String {
        return "$userPrompt。限定生成红色的中国平面剪纸图案，单色红纸，白色或透明镂空区域，平面构图，适合剪刻，不要照片质感、3D、渐变、多色涂绘或复杂背景。"
    }
}

