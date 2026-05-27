package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "official_infos",
    indexes = [
        Index(name = "idx_official_info_status_category", columnList = "status, category"),
        Index(name = "idx_official_info_publish_date", columnList = "publish_date")
    ]
)
data class OfficialInfo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 40)
    val category: String = OFFICIAL_INFO_CATEGORY_HOT,

    @Column(nullable = false, length = 120)
    val title: String = "",

    @Column(nullable = false, length = 500)
    val summary: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String = "",

    @Column(name = "cover_image_url", nullable = false, length = 500)
    val coverImageUrl: String = "",

    @Column(name = "video_url", length = 500)
    val videoUrl: String = "",

    @Column(name = "publish_date", nullable = false)
    val publishDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, length = 20)
    val status: String = OFFICIAL_INFO_STATUS_PUBLISHED,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    val createTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now()
)

const val OFFICIAL_INFO_CATEGORY_HOT = "HOT"
const val OFFICIAL_INFO_CATEGORY_INTERVIEW_VIDEO = "INTERVIEW_VIDEO"
const val OFFICIAL_INFO_CATEGORY_ACTIVITY = "ACTIVITY"
const val OFFICIAL_INFO_CATEGORY_ARTICLE = "ARTICLE"

const val OFFICIAL_INFO_STATUS_PUBLISHED = "PUBLISHED"
const val OFFICIAL_INFO_STATUS_ARCHIVED = "ARCHIVED"

val OFFICIAL_INFO_CATEGORIES = listOf(
    OFFICIAL_INFO_CATEGORY_HOT,
    OFFICIAL_INFO_CATEGORY_INTERVIEW_VIDEO,
    OFFICIAL_INFO_CATEGORY_ACTIVITY,
    OFFICIAL_INFO_CATEGORY_ARTICLE
)
