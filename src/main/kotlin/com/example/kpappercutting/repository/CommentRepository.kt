package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    // 根据动态 ID 获取所有评论
    fun findByPostIdOrderByCreateTimeAsc(postId: Long): List<Comment>
}