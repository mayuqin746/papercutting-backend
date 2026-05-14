package com.example.kpappercutting.repository

import com.example.kpappercutting.model.UserFollow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserFollowRepository : JpaRepository<UserFollow, Long> {
    fun findByFollowerIdAndFollowingId(followerId: Long, followingId: Long): UserFollow?
    fun countByFollowerId(followerId: Long): Long
    fun countByFollowingId(followingId: Long): Long
    fun findByFollowerId(followerId: Long): List<UserFollow>
    fun findByFollowingId(followingId: Long): List<UserFollow>
}
