package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

const val OPERATION_BANNER_HOME = "HOME"
const val OPERATION_BANNER_CULTURE = "CULTURE"

val OPERATION_BANNER_PLACEMENTS = setOf(
    OPERATION_BANNER_HOME,
    OPERATION_BANNER_CULTURE
)

@Entity
@Table(name = "operation_banners")
data class OperationBanner(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(length = 30, nullable = false)
    val placement: String = OPERATION_BANNER_HOME,

    @Column(nullable = false, length = 500)
    val imageUrl: String = "",

    val sortOrder: Int = 0,

    var enabled: Boolean = true,

    val createTime: LocalDateTime = LocalDateTime.now(),

    var updateTime: LocalDateTime = LocalDateTime.now()
)
