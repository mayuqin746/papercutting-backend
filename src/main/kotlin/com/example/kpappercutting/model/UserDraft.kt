package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_drafts")
data class UserDraft(
    @Id
    @Column(length = 64)
    val draftId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null,

    @Column(length = 120)
    val title: String = "",

    @Column(length = 255)
    val thumbnailUrl: String = "",

    @Column(length = 255)
    val draftUrl: String = "",

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val paperColor: Int = 0,

    @Column(length = 40)
    val foldMode: String = "",

    @Column(length = 40)
    val canvasMode: String = "",

    val isFolded: Boolean = false
)
