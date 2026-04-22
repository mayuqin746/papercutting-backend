package com.example.kpappercutting.repository

import com.example.kpappercutting.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    // Spring 会自动帮你实现按用户名查询的逻辑，神奇吧？
    fun findByUsername(username: String): User?
}