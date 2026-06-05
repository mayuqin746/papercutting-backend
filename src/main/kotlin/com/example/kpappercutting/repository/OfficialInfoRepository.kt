package com.example.kpappercutting.repository

import com.example.kpappercutting.model.OfficialInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface OfficialInfoRepository : JpaRepository<OfficialInfo, Long>, JpaSpecificationExecutor<OfficialInfo> {
    fun findByStatusOrderBySortOrderDescPublishDateDescIdDesc(status: String): List<OfficialInfo>
    fun findAllByOrderBySortOrderDescPublishDateDescIdDesc(): List<OfficialInfo>
}
