package com.example.kpappercutting.controller

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.PostReport
import com.example.kpappercutting.model.User
import com.example.kpappercutting.repository.PostReportRepository
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserRepository
import com.example.kpappercutting.security.currentUserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val postReportRepository: PostReportRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    @PostMapping("/posts")
    fun createPostReport(
        request: HttpServletRequest,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val postId = (body["postId"] as? Number)?.toLong()
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少postId"))
        val reporterId = request.currentUserId()
        val reason = (body["reason"] as? String)?.trim()?.take(80).orEmpty()
        val description = (body["description"] as? String)?.trim()?.take(500).orEmpty()

        if (reason.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "请选择举报原因"))
        }

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "动态不存在"))
        val reporter = userRepository.findById(reporterId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "举报用户不存在"))

        val report = postReportRepository.save(
            PostReport(
                post = post,
                reporter = reporter,
                reason = reason,
                description = description
            )
        )

        return ResponseEntity.ok(mapOf("status" to "success", "reportId" to report.id))
    }

    @GetMapping("/posts/admin")
    fun getPostReportsForAdmin(
        @RequestParam(required = false) status: String?
    ): List<PostReportResponse> {
        val reports = when (status) {
            "pending", "reviewed" -> postReportRepository.findByReviewStatusOrderByCreateTimeDesc(status)
            else -> postReportRepository.findAllByOrderByCreateTimeDesc()
        }
        return reports.map(::toPostReportResponse)
    }

    @PostMapping("/posts/{reportId}/review")
    fun reviewPostReport(
        @PathVariable reportId: Long,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val reportResult = body["reportResult"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少举报结果"))
        val postAction = body["postAction"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少动态处理动作"))

        if (reportResult !in setOf("valid", "invalid")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "举报结果不合法"))
        }
        if (postAction !in setOf("keep", "reject", "approve")) {
            return ResponseEntity.badRequest().body(mapOf("message" to "动态处理动作不合法"))
        }

        val report = postReportRepository.findById(reportId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        report.reviewStatus = "reviewed"
        report.reportResult = reportResult
        report.postAction = postAction
        report.reviewTime = LocalDateTime.now()

        report.post?.let { post ->
            when (postAction) {
                "reject" -> post.status = 2
                "approve" -> post.status = 1
            }
            postRepository.save(post)
        }

        postReportRepository.save(report)
        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    private fun toPostReportResponse(report: PostReport): PostReportResponse {
        return PostReportResponse(
            id = report.id,
            post = report.post,
            reporter = report.reporter,
            reason = report.reason,
            description = report.description,
            reviewStatus = report.reviewStatus,
            reportResult = report.reportResult,
            postAction = report.postAction,
            createTime = report.createTime,
            reviewTime = report.reviewTime
        )
    }
}

data class PostReportResponse(
    val id: Long,
    val post: Post?,
    val reporter: User?,
    val reason: String,
    val description: String,
    val reviewStatus: String,
    val reportResult: String?,
    val postAction: String?,
    val createTime: LocalDateTime,
    val reviewTime: LocalDateTime?
)
