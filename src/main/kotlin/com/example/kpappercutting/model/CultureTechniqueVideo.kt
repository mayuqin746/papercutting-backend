package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "culture_technique_videos",
    indexes = [
        Index(name = "idx_culture_technique_enabled_sort", columnList = "enabled,sort_order")
    ]
)
data class CultureTechniqueVideo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 120)
    val title: String = "",

    @Column(nullable = false, length = 800)
    val description: String = "",

    @Column(nullable = false, length = 500)
    val videoUrl: String = "",

    @Column(name = "sort_order")
    val sortOrder: Int = 0,

    var enabled: Boolean = true,

    val createTime: LocalDateTime = LocalDateTime.now(),

    var updateTime: LocalDateTime = LocalDateTime.now()
)
