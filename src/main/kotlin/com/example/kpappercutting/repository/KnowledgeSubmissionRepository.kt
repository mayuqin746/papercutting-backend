package com.example.kpappercutting.repository

import com.example.kpappercutting.model.KnowledgeSubmission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeSubmissionRepository : JpaRepository<KnowledgeSubmission, Long> {
    fun findByUserIdOrderByCreateTimeDesc(userId: Long): List<KnowledgeSubmission>
    fun findAllByOrderByCreateTimeDesc(): List<KnowledgeSubmission>
    fun findByStatusOrderByCreateTimeDesc(status: String): List<KnowledgeSubmission>
}
