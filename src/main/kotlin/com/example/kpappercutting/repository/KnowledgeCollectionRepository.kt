package com.example.kpappercutting.repository

import com.example.kpappercutting.model.KnowledgeCollection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeCollectionRepository : JpaRepository<KnowledgeCollection, Long> {
    fun findByUserIdAndKnowledgeId(userId: Long, knowledgeId: Long): KnowledgeCollection?
    fun findByUserIdOrderByCreateTimeAsc(userId: Long): List<KnowledgeCollection>
}
