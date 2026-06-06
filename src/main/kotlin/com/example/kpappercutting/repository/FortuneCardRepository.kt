package com.example.kpappercutting.repository

import com.example.kpappercutting.model.FortuneCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface FortuneCardRepository : JpaRepository<FortuneCard, Long>, JpaSpecificationExecutor<FortuneCard> {
    fun findByDisplayDate(displayDate: LocalDate): FortuneCard?
    @Query(
        """
        select f from FortuneCard f
        where f.displayDate = :displayDate
          and (f.status is null or f.status = :status)
        """
    )
    fun findPublishedByDisplayDate(
        @Param("displayDate") displayDate: LocalDate,
        @Param("status") status: String
    ): FortuneCard?
    fun findAllByOrderByDisplayDateDescIdDesc(): List<FortuneCard>
}
