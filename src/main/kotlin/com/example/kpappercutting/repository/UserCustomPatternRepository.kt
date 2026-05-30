package com.example.kpappercutting.repository

import com.example.kpappercutting.model.UserCustomPattern
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserCustomPatternRepository : JpaRepository<UserCustomPattern, String> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: Long): List<UserCustomPattern>
    fun findByPatternIdAndUserId(patternId: String, userId: Long): UserCustomPattern?
}
