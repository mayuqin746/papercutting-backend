package com.example.kpappercutting.model

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val author: User? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_comment_id")
    val parentComment: Comment? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reply_to_user_id")
    val replyToUser: User? = null,

    @Column(columnDefinition = "TEXT")
    val content: String = "",

    var likeCount: Int = 0,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createTime: LocalDateTime = LocalDateTime.now(),
)
