package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "knowledge_answer_records",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "knowledge_id"])]
)
data class KnowledgeAnswerRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,
    @Column(name = "knowledge_id", nullable = false)
    val knowledgeId: Long = 0,
    val selectedAnswer: String = "",
    val isCorrect: Boolean = false,
    val answeredAt: LocalDateTime = LocalDateTime.now()
)
