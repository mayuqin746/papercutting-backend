package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Knowledge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeRepository : JpaRepository<Knowledge, Long>