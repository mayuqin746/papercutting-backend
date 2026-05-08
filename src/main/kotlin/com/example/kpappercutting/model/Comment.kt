package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 修改：关联到 Post 对象，而不是直接存 Long ID
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val author: User? = null,

    @Column(columnDefinition = "TEXT")
    val content: String = "",

    val createTime: LocalDateTime = LocalDateTime.now()
)