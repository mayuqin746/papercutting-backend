package com.example.kpappercutting.controller

import com.example.kpappercutting.model.InteractionNotification
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.InteractionNotificationRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationRepository: InteractionNotificationRepository
) {
    @GetMapping
    fun listNotifications(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): List<InteractionNotificationResponse> {
        return notificationRepository.findByRecipientIdOrderByCreateTimeDesc(request.currentUserId())
            .map(::toResponse)
    }

    @GetMapping("/unread-count")
    fun unreadCount(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): Map<String, Long> {
        return mapOf("count" to notificationRepository.countByRecipientIdAndIsReadFalse(request.currentUserId()))
    }

    @PostMapping("/mark-read")
    @Transactional
    fun markRead(
        request: HttpServletRequest,
        @RequestParam(required = false) userId: Long?
    ): Map<String, Any> {
        notificationRepository.markAllRead(request.currentUserId())
        return mapOf("status" to "success")
    }

    private fun toResponse(notification: InteractionNotification): InteractionNotificationResponse {
        val post = notification.post
        val imageUrl = post?.imageUrls
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
            ?: post?.imageUrl
        return InteractionNotificationResponse(
            id = notification.id,
            type = notification.type,
            actor = notification.actor,
            postId = post?.id,
            postImageUrl = imageUrl,
            commentId = notification.commentId,
            commentContent = notification.commentContent,
            isRead = notification.isRead,
            createTime = notification.createTime
        )
    }
}

data class InteractionNotificationResponse(
    val id: Long,
    val type: String,
    val actor: User?,
    val postId: Long?,
    val postImageUrl: String?,
    val commentId: Long?,
    val commentContent: String?,
    val isRead: Boolean,
    val createTime: LocalDateTime
)
