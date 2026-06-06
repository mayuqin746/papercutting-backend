package com.example.kpappercutting.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class FortuneCardSchemaMigrator(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {
    @Volatile
    private var statusColumnChecked = false

    override fun run(args: ApplicationArguments) {
        ensureStatusColumn()
    }

    fun ensureStatusColumn() {
        if (statusColumnChecked) return
        synchronized(this) {
            if (statusColumnChecked) return
            val tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'fortune_cards'
                """.trimIndent(),
                Int::class.java
            ) ?: 0
            if (tableCount == 0) return

            val count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'fortune_cards'
                  AND COLUMN_NAME = 'status'
                """.trimIndent(),
                Int::class.java
            ) ?: 0
            if (count == 0) {
                jdbcTemplate.execute("ALTER TABLE fortune_cards ADD COLUMN status VARCHAR(20) DEFAULT 'PUBLISHED'")
            }
            jdbcTemplate.update("UPDATE fortune_cards SET status = 'PUBLISHED' WHERE status IS NULL OR status = ''")
            statusColumnChecked = true
        }
    }
}
