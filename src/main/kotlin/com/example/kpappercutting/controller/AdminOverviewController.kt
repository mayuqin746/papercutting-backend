package com.example.kpappercutting.controller

import com.example.kpappercutting.repository.ChallengeRepository
import com.example.kpappercutting.repository.KnowledgeSubmissionRepository
import com.example.kpappercutting.repository.PostReportRepository
import com.example.kpappercutting.repository.PostRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/admin")
class AdminOverviewController(
    private val postRepository: PostRepository,
    private val postReportRepository: PostReportRepository,
    private val knowledgeSubmissionRepository: KnowledgeSubmissionRepository,
    private val challengeRepository: ChallengeRepository
) {
    @GetMapping("/overview")
    fun getOverview(): AdminOverviewResponse {
        val totalPosts = postRepository.count()
        val pendingPosts = postRepository.countByStatus(0)
        val approvedPosts = postRepository.countByStatus(1)
        val rejectedPosts = postRepository.countByStatus(2)
        val pendingReports = postReportRepository.countByReviewStatus("pending")
        val pendingKnowledgeSubmissions = knowledgeSubmissionRepository.countByStatus(KNOWLEDGE_SUBMISSION_PENDING)
        val activeChallenges = challengeRepository.countByStatusAndDeadlineAfter(
            CHALLENGE_STATUS_PUBLISHED,
            LocalDateTime.now()
        )

        return AdminOverviewResponse(
            totalPosts = totalPosts,
            pendingPosts = pendingPosts,
            approvedPosts = approvedPosts,
            rejectedPosts = rejectedPosts,
            reviewedPosts = approvedPosts + rejectedPosts,
            pendingReports = pendingReports,
            pendingKnowledgeSubmissions = pendingKnowledgeSubmissions,
            activeChallenges = activeChallenges
        )
    }
}

data class AdminOverviewResponse(
    val totalPosts: Long,
    val pendingPosts: Long,
    val approvedPosts: Long,
    val rejectedPosts: Long,
    val reviewedPosts: Long,
    val pendingReports: Long,
    val pendingKnowledgeSubmissions: Long,
    val activeChallenges: Long
)
