//接口：按用户 ID 获取该用户发布的所有动态
package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Long> {
    // 修改原有的查询，只查已通过的 (status = 1)
    fun findAllByStatusOrderByCreateTimeDesc(status: Int): List<Post>

    fun findByAuthorIdOrderByCreateTimeDesc(authorId: Long): List<Post>
}