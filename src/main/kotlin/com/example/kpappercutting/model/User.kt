//数据库，记录在硬盘里的永久原始数据。

package com.example.kpappercutting.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val username: String = "",
    @Column(nullable = false)
    val password: String = "",

    val nickname: String = "",
    val region: String = "",
    val bio: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likedCount: Int = 0,

    // --- 修改为 String? 并允许为 null ---
    var avatarUrl: String? = null,
    var backgroundUrl: String? = null
)