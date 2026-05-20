package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "challenges",
    indexes = [
        Index(name = "idx_challenges_status_start", columnList = "status,start_time")
    ]
)
data class Challenge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(length = 120)
    val title: String = "",

    @Column(name = "activity_label", length = 80)
    val activityLabel: String = "",

    @Column(name = "challenge_tag", length = 80)
    val challengeTag: String = "",

    @Column(columnDefinition = "TEXT")
    val description: String = "",

    @Column(name = "inspiration_image_urls", columnDefinition = "TEXT")
    val inspirationImageUrls: String = "",

    @Column(name = "start_time")
    val startTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deadline")
    val deadline: LocalDateTime = LocalDateTime.now(),

    @Column(length = 20)
    val status: String = "DRAFT",

    val createTime: LocalDateTime = LocalDateTime.now()
)
