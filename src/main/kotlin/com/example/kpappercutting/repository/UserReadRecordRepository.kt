package com.example.kpappercutting.repository

import com.example.kpappercutting.model.UserReadRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserReadRecordRepository : JpaRepository<UserReadRecord, Long> {
    fun findByUserId(userId: Long): List<UserReadRecord>
    fun findByUserIdAndKnowledgeId(userId: Long, knowledgeId: Long): UserReadRecord?
}