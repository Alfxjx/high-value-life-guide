package com.hvlg.guide.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * 从 assets/content.json 读取构建期解析好的全书内容，并建好内存索引。
 * content.json 由 scripts/parse_content.py 在构建前生成。
 */
class ContentRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Volatile
    private var cached: GuideContent? = null

    suspend fun load(): GuideContent {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: context.assets.open(ASSET_NAME).use { input ->
                val text = input.readBytes().decodeToString()
                json.decodeFromString(GuideContent.serializer(), text)
            }.also { cached = it }
        }
    }

    companion object {
        const val ASSET_NAME = "content.json"
    }
}

/** 卡片按节分组后的索引，供 UI 直接使用。 */
class ContentIndex(content: GuideContent) {
    val sections: List<Section> = content.sections
    val cards: List<GuideCard> = content.cards
    val cardsBySection: Map<String, List<GuideCard>> = content.cards.groupBy { it.secId }
    val sectionById: Map<String, Section> = content.sections.associateBy { it.id }
    val cardById: Map<String, GuideCard> = content.cards.associateBy { it.id }

    /** 每张卡片在「全量、无筛选」列表里的位置（节头也算一个 item）。 */
    val fullListIndexById: Map<String, Int> = buildMap {
        var index = 0
        for (section in sections) {
            index++ // 节头
            for (card in cardsBySection[section.id].orEmpty()) {
                put(card.id, index)
                index++
            }
        }
    }
}
