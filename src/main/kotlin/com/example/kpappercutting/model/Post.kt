package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
data class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 建立多对一关联：多个 Post 对应一个 User
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id") // 数据库里依然叫 user_id
    val author: User? = null,

    @Column(columnDefinition = "TEXT")
    val content: String = "",
    @Column(length = 120)
    val category: String = "",
    val imageUrl: String? = null,
    @Column(columnDefinition = "TEXT")
    val imageUrls: String? = "",
    val showLocation: Boolean = false,
    @Column(length = 80)
    val locationName: String = "",
    @Column(length = 20)
    val shareType: String = "RESULT",
    @Column(length = 255)
    val draftUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createTime: LocalDateTime = LocalDateTime.now(),
    // 新增状态字段，设置默认值为 0 (待审核)
    var status: Int = 0

)
