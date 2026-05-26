package com.example.kpappercutting.repository

import com.example.kpappercutting.model.KnowledgeAnswerRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeAnswerRecordRepository : JpaRepository<KnowledgeAnswerRecord, Long> {
    fun findByUserIdAndKnowledgeId(userId: Long, knowledgeId: Long): KnowledgeAnswerRecord?
}
