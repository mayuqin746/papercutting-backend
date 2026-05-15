package com.example.kpappercutting.controller

import com.example.kpappercutting.model.UserReadRecord
import com.example.kpappercutting.repository.KnowledgeRepository
import com.example.kpappercutting.repository.UserReadRecordRepository
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/knowledge")
class KnowledgeController(
    private val knowledgeRepository: KnowledgeRepository,
    private val readRecordRepository: UserReadRecordRepository
) {

    /**
     * 获取科普列表
     * 逻辑：
     * 1. 没读过的：正常下发，isRead = false
     * 2. 今天读过的：下发，isRead = true (前端会变灰)
     * 3. 昨天或更早读过的：直接过滤掉 (前端看不见，实现消失)
     */
    @GetMapping("/all")
    fun getAllKnowledge(@RequestParam userId: Long): ResponseEntity<List<Map<String, Any>>> {
        // 1. 获取所有知识点（按ID排序）
        val allKnowledge = knowledgeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))

        // 2. 获取该用户的所有阅读记录
        val userRecords = readRecordRepository.findByUserId(userId)
        val today = LocalDate.now()

        // 3. 过滤与组装
        val result = allKnowledge.mapNotNull { k ->
            val record = userRecords.find { it.knowledgeId == k.id }

            if (record == null) {
                // 情况1：没读过
                mapOf("data" to k, "isRead" to false)
            } else {
                val readDate = record.readAt.toLocalDate()
                if (readDate.isEqual(today)) {
                    // 情况2：今天刚读过，下发但标记已读
                    mapOf("data" to k, "isRead" to true)
                } else {
                    // 情况3：以前读过的，直接过滤掉，不出现在列表里
                    null
                }
            }
        }

        return ResponseEntity.ok(result)
    }

    /**
     * 标记为已读
     */
    @PostMapping("/read")
    fun markAsRead(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val userId = body["userId"] ?: return ResponseEntity.badRequest().body("缺少userId")
        val knowledgeId = body["knowledgeId"] ?: return ResponseEntity.badRequest().body("缺少knowledgeId")

        // 检查是否已经存在记录，不存在则保存
        val existing = readRecordRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (existing == null) {
            readRecordRepository.save(UserReadRecord(userId = userId, knowledgeId = knowledgeId))
        }

        return ResponseEntity.ok(mapOf("message" to "success"))
    }
}