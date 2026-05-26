package com.example.kpappercutting.service

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException

@Service
class SeedreamImageGateway(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${ARK_API_KEY:}") private val apiKey: String,
    @Value("\${ARK_IMAGE_GENERATION_URL:https://ark.cn-beijing.volces.com/api/v3/images/generations}")
    private val generationUrl: String,
    @Value("\${ARK_IMAGE_MODEL:doubao-seedream-5-0-260128}") private val model: String
) : AiImageGateway {
    private val restTemplate = restTemplateBuilder.build()
    override fun generateOne(request: SeedreamImageRequest): String {
        if (apiKey.isBlank()) {
            throw AiImageGenerationException("未配置 ARK_API_KEY", HttpStatus.INTERNAL_SERVER_ERROR)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(apiKey)
        }
        val body = mutableMapOf<String, Any>(
            "model" to model,
            "prompt" to request.prompt,
            "response_format" to "b64_json",
            "watermark" to false,
            "size" to "2K"
        )
        request.imageDataUrl?.let { body["image"] = it }

        try {
            val response = restTemplate.exchange(
                generationUrl,
                HttpMethod.POST,
                HttpEntity(body, headers),
                JsonNode::class.java
            )
            val root = response.body
                ?: throw AiImageGenerationException("AI 服务返回为空")
            return parseB64Json(root)
        } catch (e: HttpStatusCodeException) {
            val detail = e.responseBodyAsString.take(240).ifBlank { e.statusText }
            throw AiImageGenerationException(
                message = "AI 服务返回 ${e.statusCode.value()}：$detail",
                status = HttpStatus.BAD_GATEWAY
            )
        }
    }

    private fun parseB64Json(root: JsonNode): String {
        val firstImage = root.path("data").firstOrNull()
            ?: throw AiImageGenerationException("AI 服务没有返回图片结果")
        val b64Json = firstImage.path("b64_json").asText("")
        if (b64Json.isBlank()) {
            throw AiImageGenerationException("AI 服务返回结果缺少 b64_json")
        }
        return b64Json
    }
}

