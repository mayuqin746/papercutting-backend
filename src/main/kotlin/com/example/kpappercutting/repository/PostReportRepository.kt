package com.example.kpappercutting.repository

import com.example.kpappercutting.model.PostReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PostReportRepository : JpaRepository<PostReport, Long>, JpaSpecificationExecutor<PostReport> {
    fun findAllByOrderByCreateTimeDesc(): List<PostReport>
    fun findByReviewStatusOrderByCreateTimeDesc(reviewStatus: String): List<PostReport>
    fun countByReviewStatus(reviewStatus: String): Long
}
