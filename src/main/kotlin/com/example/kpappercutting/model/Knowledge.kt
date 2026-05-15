package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "knowledge")
data class Knowledge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String = "",
    @Column(columnDefinition = "TEXT") // 支持长文本
    val content: String = "",
    val createTime: java.time.LocalDateTime = java.time.LocalDateTime.now()
)