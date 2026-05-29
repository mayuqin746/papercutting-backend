package com.example.kpappercutting.repository

import com.example.kpappercutting.model.UserDraft
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDraftRepository : JpaRepository<UserDraft, String> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: Long): List<UserDraft>
    fun findByDraftIdAndUserId(draftId: String, userId: Long): UserDraft?
}
