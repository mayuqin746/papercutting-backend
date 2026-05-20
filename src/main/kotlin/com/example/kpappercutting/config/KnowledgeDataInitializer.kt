package com.example.kpappercutting.config

import com.example.kpappercutting.model.KnowledgeItem
import com.example.kpappercutting.repository.KnowledgeItemRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KnowledgeDataInitializer {
    @Bean
    fun seedKnowledgeItems(repository: KnowledgeItemRepository) = CommandLineRunner {
        if (repository.count() > 0) return@CommandLineRunner

        repository.saveAll(
            listOf(
                KnowledgeItem(1, "剪纸指路", "陕北、山西一带，过去窑洞门上贴不同剪纸，外人一看就知道：这家是新婚、生子、祝寿，还是办丧事，相当于古代的家庭状态公告牌。", 1),
                KnowledgeItem(2, "生肖信仰", "剪纸中最常见的题材之一是十二生肖。古人相信把生肖剪出来贴在窗户上，能祈求风调雨顺、祛邪消灾。", 2),
                KnowledgeItem(3, "水传剪纸", "有一种罕见的水传剪纸，是将剪好的图案放在水面上，利用水的张力让它展开，极具观赏性。", 3),
                KnowledgeItem(4, "非遗传承", "中国剪纸在2009年被联合国教科文组织列入人类非物质文化遗产代表作名录。", 4)
            )
        )
    }
}