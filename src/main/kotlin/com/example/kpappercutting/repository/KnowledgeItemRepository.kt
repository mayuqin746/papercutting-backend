package com.example.kpappercutting.repository

import com.example.kpappercutting.model.KnowledgeItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeItemRepository : JpaRepository<KnowledgeItem, Int> {
    fun findAllByOrderBySortOrderAsc(): List<KnowledgeItem>
}
