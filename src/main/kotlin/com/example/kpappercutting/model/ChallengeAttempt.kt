package com.example.kpappercutting.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "challenge_attempts",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_challenge_attempt_user", columnNames = ["challenge_id", "user_id"])
    ],
    indexes = [
        Index(name = "idx_challenge_attempt_user", columnList = "user_id")
    ]
)
data class ChallengeAttempt(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "challenge_id")
    val challengeId: Long = 0,

    @Column(name = "user_id")
    val userId: Long = 0,

    val attemptTime: LocalDateTime = LocalDateTime.now()
)
