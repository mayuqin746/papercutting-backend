//接口：按用户 ID 获取该用户发布的所有动态
package com.example.kpappercutting.repository

import com.example.kpappercutting.model.Post
import com.example.kpappercutting.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PostRepository : JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    // 修改原有的查询，只查已通过的 (status = 1)
    fun findAllByStatusOrderByCreateTimeDesc(status: Int): List<Post>

    fun findAllByStatus(status: Int, pageable: Pageable): Page<Post>

    fun findAllByStatusOrderByLikeCountDescCreateTimeDesc(status: Int): List<Post>

    fun findAllByOrderByCreateTimeDesc(): List<Post>

    fun findByAuthorIdOrderByCreateTimeDesc(authorId: Long): List<Post>

    fun findByAuthorId(authorId: Long, pageable: Pageable): Page<Post>

    fun countByStatus(status: Int): Long

    @Query(
        value = """
            select new com.example.kpappercutting.repository.UserPostReviewGroupRow(
                p.author,
                count(p),
                coalesce(sum(case when p.status = 0 then 1L else 0L end), 0L),
                coalesce(sum(case when p.status = 1 then 1L else 0L end), 0L),
                coalesce(sum(case when p.status = 2 then 1L else 0L end), 0L),
                max(p.createTime)
            )
            from Post p
            left join p.author a
            where (:status is null or p.status = :status)
              and (
                :keyword = ''
                or lower(p.content) like lower(concat('%', :keyword, '%'))
                or lower(p.category) like lower(concat('%', :keyword, '%'))
                or lower(p.locationName) like lower(concat('%', :keyword, '%'))
                or lower(a.nickname) like lower(concat('%', :keyword, '%'))
                or lower(a.username) like lower(concat('%', :keyword, '%'))
              )
            group by p.author
            order by coalesce(sum(case when p.status = 0 then 1L else 0L end), 0L) desc, max(p.createTime) desc
        """,
        countQuery = """
            select count(distinct p.author.id)
            from Post p
            left join p.author a
            where (:status is null or p.status = :status)
              and (
                :keyword = ''
                or lower(p.content) like lower(concat('%', :keyword, '%'))
                or lower(p.category) like lower(concat('%', :keyword, '%'))
                or lower(p.locationName) like lower(concat('%', :keyword, '%'))
                or lower(a.nickname) like lower(concat('%', :keyword, '%'))
                or lower(a.username) like lower(concat('%', :keyword, '%'))
              )
        """
    )
    fun findAdminUserGroups(
        @Param("status") status: Int?,
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<UserPostReviewGroupRow>

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.likeCount), 0) from Post p where p.author.id = :authorId")
    fun sumLikeCountByAuthorId(@org.springframework.data.repository.query.Param("authorId") authorId: Long): Long
}

data class UserPostReviewGroupRow(
    val author: User?,
    val totalCount: Long,
    val pendingCount: Long,
    val approvedCount: Long,
    val rejectedCount: Long,
    val latestPostTime: LocalDateTime?
)
