package com.example.kpappercutting.service

import com.example.kpappercutting.model.AiImageGenerationMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiImageGenerationServiceTest {
    @Test
    fun `text to image calls gateway twice`() {
        val gateway = FakeGateway()
        val service = AiImageGenerationService(gateway)

        val response = service.generate(
            prompt = "窗花里的鲤鱼",
            mode = AiImageGenerationMode.TEXT_TO_IMAGE,
            count = 2,
            imageDataUrl = null
        )

        assertEquals(2, response.images.size)
        assertEquals(2, gateway.requests.size)
        assertTrue(gateway.requests.all { it.imageDataUrl == null })
    }

    @Test
    fun `image to image requires reference image and returns one image`() {
        val service = AiImageGenerationService(FakeGateway())

        assertThrows(IllegalArgumentException::class.java) {
            service.generate(
                prompt = "做成窗花",
                mode = AiImageGenerationMode.IMAGE_TO_IMAGE,
                count = 1,
                imageDataUrl = null
            )
        }

        val gateway = FakeGateway()
        val response = AiImageGenerationService(gateway).generate(
            prompt = "做成窗花",
            mode = AiImageGenerationMode.IMAGE_TO_IMAGE,
            count = 1,
            imageDataUrl = "data:image/png;base64,abc"
        )
        assertEquals(1, response.images.size)
        assertEquals("data:image/png;base64,abc", gateway.requests.single().imageDataUrl)
    }

    @Test
    fun `prompt appends red chinese paper cut constraints`() {
        val gateway = FakeGateway()
        AiImageGenerationService(gateway).generate(
            prompt = "小猫抱锦鲤",
            mode = AiImageGenerationMode.TEXT_TO_IMAGE,
            count = 2,
            imageDataUrl = null
        )

        val prompt = gateway.requests.first().prompt
        assertTrue(prompt.contains("红色的中国平面剪纸"))
        assertTrue(prompt.contains("不要照片质感"))
    }

    @Test
    fun `text to image rejects wrong count`() {
        val service = AiImageGenerationService(FakeGateway())

        assertThrows(IllegalArgumentException::class.java) {
            service.generate(
                prompt = "龙纹",
                mode = AiImageGenerationMode.TEXT_TO_IMAGE,
                count = 1,
                imageDataUrl = null
            )
        }
    }

    @Test
    fun `blank gateway image becomes clear ai error`() {
        val service = AiImageGenerationService(FakeGateway(returnBlank = true))

        assertThrows(AiImageGenerationException::class.java) {
            service.generate(
                prompt = "龙纹",
                mode = AiImageGenerationMode.TEXT_TO_IMAGE,
                count = 2,
                imageDataUrl = null
            )
        }
    }

    private class FakeGateway(
        private val returnBlank: Boolean = false
    ) : AiImageGateway {
        val requests = mutableListOf<SeedreamImageRequest>()

        override fun generateOne(request: SeedreamImageRequest): String {
            requests += request
            return if (returnBlank) "" else "image-${requests.size}"
        }
    }
}

