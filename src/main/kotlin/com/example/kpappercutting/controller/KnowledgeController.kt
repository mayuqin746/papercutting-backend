// src/main/kotlin/com/example/kpappercutting/controller/KnowledgeController.kt
package com.example.kpappercutting.controller

import com.example.kpappercutting.model.KnowledgeCollection
import com.example.kpappercutting.repository.KnowledgeCollectionRepository
import com.example.kpappercutting.repository.KnowledgeRepository
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/knowledge")
class KnowledgeController(
    private val knowledgeRepository: KnowledgeRepository,
    private val collectionRepository: KnowledgeCollectionRepository
) {
    // 1. 获取主页科普列表（包含进度条统计）
    @GetMapping("/home")
    fun getHomeKnowledge(@RequestParam userId: Long): ResponseEntity<Map<String, Any>> {
        // 按添加顺序(id升序)获取所有数据
        val allKnowledge = knowledgeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
        val userCollections = collectionRepository.findByUserIdOrderByCreateTimeAsc(userId)
        val today = LocalDate.now()

        val mainList = allKnowledge.mapNotNull { k ->
            val coll = userCollections.find { it.knowledgeId == k.id }
            if (coll == null) {
                // 没点过：正常显示
                mapOf("id" to k.id, "title" to k.title, "content" to k.content, "isRead" to false)
            } else {
                val collectDate = coll.createTime.toLocalDate()
                if (collectDate.isEqual(today)) {
                    // 今天点的：显示并置灰
                    mapOf("id" to k.id, "title" to k.title, "content" to k.content, "isRead" to true)
                } else {
                    // 昨天及以前点的：主页不显示（消失）
                    null
                }
            }
        }

        return ResponseEntity.ok(mapOf(
            "totalCount" to allKnowledge.size,
            "readCount" to userCollections.size,
            "items" to mainList
        ))
    }

    // 2. 收集（点击小卡弹窗后调用）
    @PostMapping("/collect")
    fun collectKnowledge(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val userId = body["userId"] ?: return ResponseEntity.badRequest().body("missing userId")
        val knowledgeId = body["knowledgeId"] ?: return ResponseEntity.badRequest().body("missing knowledgeId")

        val existing = collectionRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (existing == null) {
            collectionRepository.save(KnowledgeCollection(userId = userId, knowledgeId = knowledgeId))
        }
        return ResponseEntity.ok(mapOf("success" to true))
    }

    // 3. 获取收集子页的全部列表
    @GetMapping("/collections/{userId}")
    fun getCollections(@PathVariable userId: Long): ResponseEntity<List<Map<String, Any>>> {
        val collections = collectionRepository.findByUserIdOrderByCreateTimeAsc(userId)
        val collectionMap = collections.associateBy { it.knowledgeId }

        // 查找对应的数据并按收集时间排序
        val collectedData = knowledgeRepository.findAllById(collectionMap.keys)
            .sortedBy { collectionMap[it.id]?.createTime }
            .map { k ->
                mapOf("id" to k.id, "title" to k.title, "content" to k.content, "isRead" to true)
            }
        return ResponseEntity.ok(collectedData)
    }
}