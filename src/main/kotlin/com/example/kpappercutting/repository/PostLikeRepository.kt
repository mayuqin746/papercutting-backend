package com.example.kpappercutting.repository

import com.example.kpappercutting.model.PostLike
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostLikeRepository : JpaRepository<PostLike, Long> {
    // 检查是否点赞过
    fun findByUserIdAndPostId(userId: Long, postId: Long): PostLike?

    // 获取用户点赞过的所有作品ID
    fun findByUserId(userId: Long): List<PostLike>

    fun findByUserIdOrderByCreateTimeDesc(userId: Long, pageable: Pageable): Page<PostLike>

    fun deleteByPostId(postId: Long)
}
