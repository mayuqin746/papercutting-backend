package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Challenge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChallengeRepository : JpaRepository<Challenge, Long>, JpaSpecificationExecutor<Challenge> {
    fun findFirstByStatusOrderByStartTimeDescIdDesc(status: String): Challenge?
    fun findByStatusOrderByStartTimeDescIdDesc(status: String): List<Challenge>
    fun findByStatusAndDeadlineAfterOrderByStartTimeDescIdDesc(status: String, deadline: LocalDateTime): List<Challenge>
    fun findAllByOrderByStartTimeDescIdDesc(): List<Challenge>
    fun countByStatusAndDeadlineAfter(status: String, deadline: LocalDateTime): Long
}
