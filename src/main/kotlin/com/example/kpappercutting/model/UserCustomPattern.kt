package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_custom_patterns")
data class UserCustomPattern(
    @Id
    @Column(length = 64)
    val patternId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null,

    @Column(length = 120)
    val displayName: String = "",

    @Column(length = 255)
    val imageUrl: String = "",

    @Column(length = 255)
    val thumbnailUrl: String = "",

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    val normalizedPathJson: String = "",

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
