package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "knowledge_collections",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "knowledge_id"])
    ]
)
data class KnowledgeCollection(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "knowledge_id", nullable = false)
    val knowledgeId: Int = 0,

    val createTime: LocalDateTime = LocalDateTime.now()
)
