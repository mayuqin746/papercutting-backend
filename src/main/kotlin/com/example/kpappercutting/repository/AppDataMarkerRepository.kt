package com.example.kpappercutting.repository

import com.example.kpappercutting.model.AppDataMarker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppDataMarkerRepository : JpaRepository<AppDataMarker, String>
