package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "post_likes", uniqueConstraints = [
    UniqueConstraint(columnNames = ["user_id", "post_id"]) // 唯一约束：一个用户对一个作品只能有一条记录
])
data class PostLike(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: Long = 0,
    val postId: Long = 0,
    val createTime: LocalDateTime = LocalDateTime.now()
)