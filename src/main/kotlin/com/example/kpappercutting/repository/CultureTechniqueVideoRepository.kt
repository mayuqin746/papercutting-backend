package com.example.kpappercutting.repository

import com.example.kpappercutting.model.CultureTechniqueVideo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface CultureTechniqueVideoRepository :
    JpaRepository<CultureTechniqueVideo, Long>,
    JpaSpecificationExecutor<CultureTechniqueVideo> {
    fun findByEnabledTrueOrderBySortOrderDescIdDesc(): List<CultureTechniqueVideo>
    fun findAllByOrderBySortOrderDescIdDesc(pageable: Pageable): Page<CultureTechniqueVideo>
}
