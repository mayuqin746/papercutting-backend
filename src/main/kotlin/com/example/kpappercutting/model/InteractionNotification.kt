package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interaction_notifications")
data class InteractionNotification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    val recipient: User? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    val actor: User? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @Column(length = 20)
    val type: String = "",

    val commentId: Long? = null,

    @Column(columnDefinition = "TEXT")
    val commentContent: String? = null,

    var isRead: Boolean = false,

    val createTime: LocalDateTime = LocalDateTime.now()
)

const val NOTIFICATION_TYPE_LIKE = "LIKE"
const val NOTIFICATION_TYPE_COMMENT = "COMMENT"
const val NOTIFICATION_TYPE_COMMENT_REPLY = "COMMENT_REPLY"
const val NOTIFICATION_TYPE_COMMENT_LIKE = "COMMENT_LIKE"
