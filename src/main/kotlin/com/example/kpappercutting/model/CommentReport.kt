package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comment_reports")
data class CommentReport(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comment_id")
    var comment: Comment? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User? = null,

    @Column(length = 80, nullable = false)
    val reason: String = "",

    @Column(columnDefinition = "TEXT")
    val description: String = "",

    @Column(length = 20, nullable = false)
    var reviewStatus: String = "pending",

    @Column(length = 20)
    var reportResult: String? = null,

    @Column(length = 20)
    var commentAction: String? = null,

    val createTime: LocalDateTime = LocalDateTime.now(),

    var reviewTime: LocalDateTime? = null
)
