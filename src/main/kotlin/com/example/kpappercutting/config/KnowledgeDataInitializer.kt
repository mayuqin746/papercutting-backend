package com.example.kpappercutting.config

import com.example.kpappercutting.controller.KNOWLEDGE_SOURCE_OFFICIAL
import com.example.kpappercutting.controller.KNOWLEDGE_STATUS_PUBLISHED
import com.example.kpappercutting.model.AppDataMarker
import com.example.kpappercutting.model.Knowledge
import com.example.kpappercutting.repository.AppDataMarkerRepository
import com.example.kpappercutting.repository.KnowledgeAnswerRecordRepository
import com.example.kpappercutting.repository.KnowledgeCollectionRepository
import com.example.kpappercutting.repository.KnowledgeRepository
import com.example.kpappercutting.repository.UserReadRecordRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class KnowledgeDataInitializer(
    private val markerRepository: AppDataMarkerRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val collectionRepository: KnowledgeCollectionRepository,
    private val readRecordRepository: UserReadRecordRepository,
    private val answerRecordRepository: KnowledgeAnswerRecordRepository
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (markerRepository.existsById(MARKER_KEY)) return

        collectionRepository.deleteAll()
        answerRecordRepository.deleteAll()
        readRecordRepository.deleteAll()
        knowledgeRepository.deleteAll()

        knowledgeRepository.save(
            Knowledge(
                category = "定义",
                title = "什么是中国民间剪纸",
                content = "中国民间剪纸是由中国劳动人民在历代民俗生活中创造、流传、享用的一种意象艺术形式。它不仅是用剪刀或刻刀在纸上剪刻纹样的手工美术品，更包含着深厚的文化内涵。更为确切地说，民间剪纸应当被称为\"民俗剪纸\"，因为其产生和发展与民俗生活密不可分。",
                questionType = "TRUE_FALSE",
                questionText = "中国民间剪纸仅仅是作为装饰用途的手工美术品，与文化习俗关系不大。（ ）",
                optionsJson = "[\"正确\",\"错误\"]",
                answer = "false",
                answerExplanation = "错误。中国民间剪纸是民俗文化的形象载体，与民俗生活密不可分。",
                sourceType = KNOWLEDGE_SOURCE_OFFICIAL,
                status = KNOWLEDGE_STATUS_PUBLISHED
            )
        )
        markerRepository.save(AppDataMarker(markerKey = MARKER_KEY))
    }

    private companion object {
        const val MARKER_KEY = "knowledge_schema_v2_seeded"
    }
}
