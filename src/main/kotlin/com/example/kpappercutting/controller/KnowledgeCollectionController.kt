package com.example.kpappercutting.controller

import com.example.kpappercutting.model.KnowledgeCollection
import com.example.kpappercutting.repository.KnowledgeCollectionRepository
import com.example.kpappercutting.repository.KnowledgeItemRepository
import com.example.kpappercutting.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/knowledge/collections")
class KnowledgeCollectionController(
    private val knowledgeCollectionRepository: KnowledgeCollectionRepository,
    private val knowledgeItemRepository: KnowledgeItemRepository,
    private val userRepository: UserRepository
) {
    @PostMapping
    fun collect(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val userId = body["userId"]
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "missing userId"))
        val knowledgeId = body["knowledgeId"]?.toInt()
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "missing knowledgeId"))

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404).body(mapOf("message" to "user not found"))
        }
        if (!knowledgeItemRepository.existsById(knowledgeId)) {
            return ResponseEntity.status(404).body(mapOf("message" to "knowledge item not found"))
        }

        val existing = knowledgeCollectionRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
        if (existing != null) {
            return ResponseEntity.ok(
                mapOf(
                    "status" to "exists",
                    "knowledgeId" to existing.knowledgeId
                )
            )
        }

        val saved = knowledgeCollectionRepository.save(
            KnowledgeCollection(userId = userId, knowledgeId = knowledgeId)
        )

        return ResponseEntity.ok(
            mapOf(
                "status" to "collected",
                "knowledgeId" to saved.knowledgeId
            )
        )
    }

    @GetMapping("/{userId}")
    fun getCollections(@PathVariable userId: Long): ResponseEntity<Any> {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404).body(mapOf("message" to "user not found"))
        }

        val knowledgeIds = knowledgeCollectionRepository
            .findByUserIdOrderByCreateTimeAsc(userId)
            .map { it.knowledgeId }

        return ResponseEntity.ok(knowledgeIds)
    }
}
