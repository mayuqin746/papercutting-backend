package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "knowledge")
data class Knowledge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(length = 20)
    val category: String = "定义",
    val title: String = "",
    @Column(columnDefinition = "TEXT")
    val content: String = "",
    @Column(length = 30)
    val questionType: String = "TRUE_FALSE",
    @Column(columnDefinition = "TEXT")
    val questionText: String = "",
    @Column(columnDefinition = "TEXT")
    val optionsJson: String = "",
    @Column(length = 120)
    val answer: String = "",
    @Column(columnDefinition = "TEXT")
    val answerExplanation: String = "",
    @Column(columnDefinition = "TEXT")
    val imageUrls: String? = null,
    @Column(length = 30)
    val sourceType: String = "OFFICIAL",
    @Column(length = 30)
    val status: String = "PUBLISHED",
    val authorSubmissionId: Long? = null,
    val createTime: LocalDateTime = LocalDateTime.now()
)
