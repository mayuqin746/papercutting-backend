//数据库，记录在硬盘里的永久原始数据。
package com.example.kpappercutting.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val username: String = "",

    // --- 修改这里：删掉 @JsonIgnore，换成下面这个 ---
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    val password: String = "",

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false, length = 100, columnDefinition = "varchar(100) default ''")
    val passwordHash: String = "",

    val nickname: String = "",
    val region: String = "",
    val bio: String = "",
    var followingCount: Int = 0,
    var followerCount: Int = 0,
    val likedCount: Int = 0,
    var avatarUrl: String? = null,
    var backgroundUrl: String? = null
)
