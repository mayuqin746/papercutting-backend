package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Knowledge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeRepository : JpaRepository<Knowledge, Long> {
    fun findByStatusOrderByIdAsc(status: String): List<Knowledge>
    fun findAllByOrderByIdAsc(): List<Knowledge>
    fun findByAuthorSubmissionId(authorSubmissionId: Long): Knowledge?
    fun findAllByAuthorSubmissionId(authorSubmissionId: Long): List<Knowledge>
}
