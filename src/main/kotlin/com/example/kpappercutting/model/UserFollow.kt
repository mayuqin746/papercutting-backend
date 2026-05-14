package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_follows",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_follow", columnNames = ["follower_id", "following_id"])
    ],
    indexes = [
        Index(name = "idx_user_follows_follower", columnList = "follower_id"),
        Index(name = "idx_user_follows_following", columnList = "following_id")
    ]
)
data class UserFollow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "follower_id", nullable = false)
    val followerId: Long = 0,

    @Column(name = "following_id", nullable = false)
    val followingId: Long = 0,

    val createTime: LocalDateTime = LocalDateTime.now()
)
