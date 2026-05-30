package com.example.kpappercutting.service

import com.example.kpappercutting.model.AiGeneratedImageResponse
import com.example.kpappercutting.model.AiImageGenerationMode
import com.example.kpappercutting.model.AiImageGenerationResponse
import com.example.kpappercutting.model.AiPaperCutTechnique
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
        if (mode == AiImageGenerationMode.TEXT_TO_IMAGE) {
            require(trimmedPrompt.isNotBlank()) { "请输入构思描述" }
        }

        val resolvedCount = expectedCountFor(mode)
        require(count == resolvedCount) {
            "AI 创作一次必须生成 2 张"
        }
        if (mode == AiImageGenerationMode.IMAGE_TO_IMAGE) {
            require(!imageDataUrl.isNullOrBlank()) { "图生图需要上传参考图片" }
        }

        val userPrompt = trimmedPrompt.ifBlank { "将参考图片转绘成剪纸风格" }
        val images = paperCutTechniques.map { technique ->
            val b64Json = gateway.generateOne(
                SeedreamImageRequest(
                    prompt = buildSeedreamPrompt(userPrompt, technique),
                    imageDataUrl = imageDataUrl.takeIf { mode == AiImageGenerationMode.IMAGE_TO_IMAGE }
                )
            )
            if (b64Json.isBlank()) {
                throw AiImageGenerationException("AI 服务没有返回图片数据")
            }
            AiGeneratedImageResponse(
                b64Json = b64Json,
                technique = technique,
                displayName = technique.displayName
            )
        }

        return AiImageGenerationResponse(
            mode = mode,
            images = images
        )
    }

    private fun expectedCountFor(mode: AiImageGenerationMode): Int {
        return when (mode) {
            AiImageGenerationMode.TEXT_TO_IMAGE -> 2
            AiImageGenerationMode.IMAGE_TO_IMAGE -> 2
        }
    }

    private fun buildSeedreamPrompt(
        userPrompt: String,
        technique: AiPaperCutTechnique
    ): String {
        val techniquePrompt = when (technique) {
            AiPaperCutTechnique.YANG_CARVING ->
                "采用阳刻手法：主体和主要线条保留为红色纸面，背景和镂空处为纯白，边缘清晰，红色区域连贯。"
            AiPaperCutTechnique.YIN_CUT ->
                "采用阴镂手法：以红色纸面为整体基底，通过纯白镂空线条和白色洞形表现主体，线条适合刀刻。"
        }
        return "$userPrompt。$techniquePrompt 限定生成红色的中国平面剪纸图案，单色红纸，纯白镂空区域，平面构图，适合剪刻，不要照片质感、3D、渐变、多色涂绘或复杂背景。"
    }

    private val paperCutTechniques = listOf(
        AiPaperCutTechnique.YANG_CARVING,
        AiPaperCutTechnique.YIN_CUT
    )
}

