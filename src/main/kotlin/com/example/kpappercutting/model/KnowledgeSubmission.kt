package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "knowledge_submissions")
data class KnowledgeSubmission(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val author: User? = null,
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
    var status: String = "PENDING",
    @Column(columnDefinition = "TEXT")
    var reviewNote: String = "",
    val createTime: LocalDateTime = LocalDateTime.now(),
    var reviewTime: LocalDateTime? = null
)
