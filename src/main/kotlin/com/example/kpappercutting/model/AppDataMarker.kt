package com.example.kpappercutting.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "app_data_markers")
data class AppDataMarker(
    @Id
    val markerKey: String = "",
    val appliedAt: LocalDateTime = LocalDateTime.now()
)
