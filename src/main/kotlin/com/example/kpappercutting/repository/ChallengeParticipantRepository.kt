package com.example.kpappercutting.repository

import com.example.kpappercutting.model.ChallengeParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChallengeParticipantRepository : JpaRepository<ChallengeParticipant, Long> {
    fun existsByChallengeIdAndUserId(challengeId: Long, userId: Long): Boolean
    fun countByChallengeId(challengeId: Long): Long
    fun deleteByChallengeId(challengeId: Long)
    fun deleteByPostId(postId: Long)
}
