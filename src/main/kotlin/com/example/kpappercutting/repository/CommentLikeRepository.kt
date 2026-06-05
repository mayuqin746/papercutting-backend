package com.example.kpappercutting.repository

import com.example.kpappercutting.model.CommentLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentLikeRepository : JpaRepository<CommentLike, Long> {
    fun findByUserIdAndCommentId(userId: Long, commentId: Long): CommentLike?

    fun findByUserIdAndCommentIdIn(userId: Long, commentIds: Collection<Long>): List<CommentLike>

    fun deleteByCommentId(commentId: Long)

    fun deleteByCommentIdIn(commentIds: Collection<Long>)
}
