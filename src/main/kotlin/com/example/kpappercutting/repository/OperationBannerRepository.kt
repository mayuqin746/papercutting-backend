package com.example.kpappercutting.repository

import com.example.kpappercutting.model.OperationBanner
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OperationBannerRepository : JpaRepository<OperationBanner, Long> {
    fun findByPlacementAndEnabledTrueOrderBySortOrderDescIdDesc(placement: String): List<OperationBanner>

    fun findByPlacementOrderBySortOrderDescIdDesc(placement: String): List<OperationBanner>

    fun findByPlacement(placement: String, pageable: Pageable): Page<OperationBanner>

    fun findAllByOrderByPlacementAscSortOrderDescIdDesc(pageable: Pageable): Page<OperationBanner>
}
