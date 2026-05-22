package com.example.kpappercutting.repository

import com.example.kpappercutting.model.FortuneCard
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface FortuneCardRepository : JpaRepository<FortuneCard, Long> {
    fun findByDisplayDate(displayDate: LocalDate): FortuneCard?
    fun findAllByOrderByDisplayDateDescIdDesc(): List<FortuneCard>
}
