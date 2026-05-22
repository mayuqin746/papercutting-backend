package com.example.kpappercutting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "fortune_card_collections",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_fortune_card_collection_user", columnNames = ["user_id", "fortune_card_id"])
    ],
    indexes = [
        Index(name = "idx_fortune_card_collection_user", columnList = "user_id")
    ]
)
data class FortuneCardCollection(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "fortune_card_id", nullable = false)
    val fortuneCardId: Long = 0,

    val collectTime: LocalDateTime = LocalDateTime.now()
)
