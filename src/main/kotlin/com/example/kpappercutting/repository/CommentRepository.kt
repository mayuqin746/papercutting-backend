package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    fun findByPost_IdOrderByCreateTimeAsc(postId: Long): List<Comment>

    fun findByParentComment_IdOrderByCreateTimeAsc(parentCommentId: Long): List<Comment>

    fun findByParentComment_IdIn(parentCommentIds: Collection<Long>): List<Comment>

    fun deleteByPost_Id(postId: Long)
}
