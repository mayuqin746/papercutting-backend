package com.example.kpappercutting.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

const val AUTH_USER_ID_ATTRIBUTE = "authUserId"
const val AUTH_USERNAME_ATTRIBUTE = "authUsername"

@Component
class AuthInterceptor(
    private val jwtService: JwtService
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) return true

        val token = request.getBearerToken()
        val jwtUser = token?.let(jwtService::parseToken)
        if (jwtUser != null) {
            request.setAttribute(AUTH_USER_ID_ATTRIBUTE, jwtUser.userId)
            request.setAttribute(AUTH_USERNAME_ATTRIBUTE, jwtUser.username)
        }

        if (!requiresLogin(request)) return true

        if (jwtUser == null) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"message":"请先登录"}""")
            return false
        }

        return true
    }

    private fun HttpServletRequest.getBearerToken(): String? {
        val header = getHeader("Authorization").orEmpty()
        return header
            .takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(" ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun requiresLogin(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val method = request.method.uppercase()

        if (!path.startsWith("/api/")) return false
        if (path == "/api/auth/login" || path == "/api/auth/register") return false

        return when {
            path == "/api/auth/update" -> true
            path == "/api/auth/upload" -> true
            path == "/api/auth/change-password" -> true
            path == "/api/posts/upload" -> true
            path == "/api/posts/upload-draft" -> true
            path == "/api/posts/create" -> true
            path == "/api/posts/like" -> true
            method == "DELETE" && Regex("^/api/posts/\\d+$").matches(path) -> true
            path == "/api/comments/create" -> true
            method == "POST" && Regex("^/api/comments/\\d+/like$").matches(path) -> true
            method == "DELETE" && Regex("^/api/comments/\\d+$").matches(path) -> true
            path.startsWith("/api/notifications") -> true
            path == "/api/reports/posts" && method == "POST" -> true
            path == "/api/reports/comments" && method == "POST" -> true
            path == "/api/users/follow" -> true
            path == "/api/users/follow/toggle" -> true
            path == "/api/fortune/collect" -> true
            path == "/api/ai/images/generate" -> true
            path.startsWith("/api/drafts") -> true
            path.startsWith("/api/custom-patterns") -> true
            path == "/api/knowledge/home" -> true
            path == "/api/knowledge/open" -> true
            path == "/api/knowledge/answer" -> true
            path == "/api/knowledge/collect" -> true
            Regex("^/api/knowledge/collections/\\d+$").matches(path) -> true
            path == "/api/knowledge/submissions" && method == "POST" -> true
            Regex("^/api/knowledge/submissions/\\d+$").matches(path) -> true
            Regex("^/api/challenges/\\d+/attempt$").matches(path) -> true
            else -> false
        }
    }
}
