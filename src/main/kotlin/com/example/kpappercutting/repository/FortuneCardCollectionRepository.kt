package com.example.kpappercutting.repository

import com.example.kpappercutting.model.FortuneCardCollection
import org.springframework.data.jpa.repository.JpaRepository

interface FortuneCardCollectionRepository : JpaRepository<FortuneCardCollection, Long> {
    fun findByUserIdAndFortuneCardId(userId: Long, fortuneCardId: Long): FortuneCardCollection?
    fun findByUserIdOrderByCollectTimeAsc(userId: Long): List<FortuneCardCollection>
    fun deleteByFortuneCardId(fortuneCardId: Long)
}
