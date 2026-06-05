package com.example.kpappercutting.repository

import com.example.kpappercutting.model.FortuneCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate

interface FortuneCardRepository : JpaRepository<FortuneCard, Long>, JpaSpecificationExecutor<FortuneCard> {
    fun findByDisplayDate(displayDate: LocalDate): FortuneCard?
    fun findAllByOrderByDisplayDateDescIdDesc(): List<FortuneCard>
}
