package com.example.kpappercutting.repository

import com.example.kpappercutting.model.ChallengeAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChallengeAttemptRepository : JpaRepository<ChallengeAttempt, Long> {
    fun findByChallengeIdAndUserId(challengeId: Long, userId: Long): ChallengeAttempt?
}
