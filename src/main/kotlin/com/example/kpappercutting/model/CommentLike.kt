package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "comment_likes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "comment_id"])
    ]
)
data class CommentLike(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: Long = 0,
    val commentId: Long = 0,
    val createTime: LocalDateTime = LocalDateTime.now()
)
