package com.example.kpappercutting.repository

import com.example.kpappercutting.model.CommentReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommentReportRepository : JpaRepository<CommentReport, Long>, JpaSpecificationExecutor<CommentReport> {
    fun countByReviewStatus(reviewStatus: String): Long

    fun deleteByComment_Id(commentId: Long)

    fun deleteByComment_IdIn(commentIds: Collection<Long>)

    fun deleteByPost_Id(postId: Long)

    @Modifying
    @Query("update CommentReport r set r.comment = null where r.comment.id in :commentIds")
    fun detachComments(@Param("commentIds") commentIds: Collection<Long>): Int
}
