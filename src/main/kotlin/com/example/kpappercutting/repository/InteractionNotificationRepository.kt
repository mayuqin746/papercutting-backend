package com.example.kpappercutting.repository

import com.example.kpappercutting.model.InteractionNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InteractionNotificationRepository : JpaRepository<InteractionNotification, Long> {
    fun findByRecipientIdOrderByCreateTimeDesc(recipientId: Long): List<InteractionNotification>

    fun countByRecipientIdAndIsReadFalse(recipientId: Long): Long

    fun deleteByRecipientIdAndActorIdAndPostIdAndType(
        recipientId: Long,
        actorId: Long,
        postId: Long,
        type: String
    )

    fun deleteByCommentIdAndType(commentId: Long, type: String)

    fun deleteByCommentIdIn(commentIds: Collection<Long>)

    fun deleteByRecipientIdAndActorIdAndPostIdAndTypeAndCommentId(
        recipientId: Long,
        actorId: Long,
        postId: Long,
        type: String,
        commentId: Long
    )

    fun deleteByPostId(postId: Long)

    @Modifying
    @Query("update InteractionNotification n set n.isRead = true where n.recipient.id = :recipientId")
    fun markAllRead(@Param("recipientId") recipientId: Long): Int
}
