package com.example.kpappercutting.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_read_records")
data class UserReadRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: Long = 0,
    val knowledgeId: Long = 0,
    val readAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)