package com.example.kpappercutting.repository

import com.example.kpappercutting.model.OfficialInfo
import org.springframework.data.jpa.repository.JpaRepository

interface OfficialInfoRepository : JpaRepository<OfficialInfo, Long> {
    fun findByStatusOrderBySortOrderDescPublishDateDescIdDesc(status: String): List<OfficialInfo>
    fun findAllByOrderBySortOrderDescPublishDateDescIdDesc(): List<OfficialInfo>
}
