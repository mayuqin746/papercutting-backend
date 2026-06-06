package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

const val FORTUNE_CARD_STATUS_PUBLISHED = "PUBLISHED"
const val FORTUNE_CARD_STATUS_ARCHIVED = "ARCHIVED"

@Entity
@Table(
    name = "fortune_cards",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_fortune_card_display_date", columnNames = ["display_date"])
    ],
    indexes = [
        Index(name = "idx_fortune_card_display_date", columnList = "display_date")
    ]
)
data class FortuneCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "display_date", nullable = false)
    val displayDate: LocalDate = LocalDate.now(),

    @Column(name = "pattern_image_url", length = 500)
    val patternImageUrl: String = "",

    @Column(name = "lunar_date", length = 80)
    val lunarDate: String = "",

    @Column(name = "solar_term", length = 80)
    val solarTerm: String = "",

    @Column(name = "suitable_events", length = 160)
    val suitableEvents: String = "",

    @Column(length = 20)
    val status: String? = FORTUNE_CARD_STATUS_PUBLISHED,

    val createTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now()
)
