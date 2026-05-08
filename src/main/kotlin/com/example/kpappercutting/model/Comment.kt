package com.example.kpappercutting.model

import com.fasterxml.jackson.annotation.JsonFormat
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

    // 在服务端的 Comment 模型中添加
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createTime: LocalDateTime = LocalDateTime.now(),

)