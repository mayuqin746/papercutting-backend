package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "knowledge_items")
data class KnowledgeItem(
    @Id
    val id: Int = 0,

    @Column(nullable = false)
    val title: String = "",

    @Column(nullable = false, length = 2000)
    val content: String = "",

    @Column(nullable = false)
    val sortOrder: Int = 0
)
