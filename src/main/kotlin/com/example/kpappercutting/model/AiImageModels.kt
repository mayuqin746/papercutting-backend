package com.example.kpappercutting.model

enum class AiImageGenerationMode {
    TEXT_TO_IMAGE,
    IMAGE_TO_IMAGE
}

data class AiGeneratedImageResponse(
    val b64Json: String
)

data class AiImageGenerationResponse(
    val mode: AiImageGenerationMode,
    val images: List<AiGeneratedImageResponse>
)

