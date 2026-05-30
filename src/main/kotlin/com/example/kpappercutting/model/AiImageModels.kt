package com.example.kpappercutting.model

enum class AiImageGenerationMode {
    TEXT_TO_IMAGE,
    IMAGE_TO_IMAGE
}

data class AiGeneratedImageResponse(
    val b64Json: String,
    val technique: AiPaperCutTechnique,
    val displayName: String
)

data class AiImageGenerationResponse(
    val mode: AiImageGenerationMode,
    val images: List<AiGeneratedImageResponse>
)

enum class AiPaperCutTechnique(
    val displayName: String
) {
    YANG_CARVING("阳刻"),
    YIN_CUT("阴镂")
}

