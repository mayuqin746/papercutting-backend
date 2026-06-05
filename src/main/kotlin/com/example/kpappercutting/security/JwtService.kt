package com.example.kpappercutting.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${jwt.secret:}")
    private val secret: String,
    @Value("\${jwt.expiration-seconds:604800}")
    private val expirationSeconds: Long,
    private val objectMapper: ObjectMapper
) {
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()

    @PostConstruct
    fun validateSecret() {
        require(secret.length >= 32) {
            "JWT_SECRET must be configured and at least 32 characters long"
        }
    }

    fun generateToken(userId: Long, username: String): String {
        val now = Instant.now().epochSecond
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val payload = mapOf(
            "sub" to userId.toString(),
            "username" to username,
            "iat" to now,
            "exp" to now + expirationSeconds
        )
        val headerPart = encodeJson(header)
        val payloadPart = encodeJson(payload)
        val signature = sign("$headerPart.$payloadPart")
        return "$headerPart.$payloadPart.$signature"
    }

    fun parseToken(token: String): JwtUser? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val signingInput = "${parts[0]}.${parts[1]}"
        if (!constantTimeEquals(sign(signingInput), parts[2])) return null

        return runCatching {
            val payloadJson = String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
            val payload = objectMapper.readValue(payloadJson, Map::class.java)
            val userId = payload["sub"]?.toString()?.toLongOrNull() ?: return null
            val username = payload["username"]?.toString().orEmpty()
            val expiresAt = (payload["exp"] as? Number)?.toLong()
                ?: payload["exp"]?.toString()?.toLongOrNull()
                ?: return null

            if (Instant.now().epochSecond >= expiresAt) return null
            JwtUser(userId = userId, username = username)
        }.getOrNull()
    }

    private fun encodeJson(value: Any): String {
        val json = objectMapper.writeValueAsBytes(value)
        return encoder.encodeToString(json)
    }

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(key)
        return encoder.encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        var result = 0
        left.indices.forEach { index ->
            result = result or (left[index].code xor right[index].code)
        }
        return result == 0
    }
}

data class JwtUser(
    val userId: Long,
    val username: String
)
