package com.example.kpappercutting.controller

import com.example.kpappercutting.model.KnowledgeItem
import com.example.kpappercutting.repository.KnowledgeItemRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("/api/knowledge")
class KnowledgeItemController(
    private val knowledgeItemRepository: KnowledgeItemRepository
) {
    @GetMapping("/items")
    fun getKnowledgeItems(): List<KnowledgeItem> {
        return knowledgeItemRepository.findAllByOrderBySortOrderAsc()
    }
}
