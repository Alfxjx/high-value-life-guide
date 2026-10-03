package com.hvlg.guide.data

import kotlinx.serialization.Serializable

@Serializable
data class GuideContent(
    val sections: List<Section> = emptyList(),
    val cards: List<GuideCard> = emptyList(),
)

@Serializable
data class Section(
    val id: String,
    val num: Int,
    val title: String,
    val intro: String = "",
    val count: Int = 0,
)

@Serializable
data class GuideCard(
    val id: String,
    val secId: String,
    val num: Int,
    val title: String,
    val grade: String = "",
    val ratio: String = "",
    val tags: Map<String, String> = emptyMap(),
    val plain: String = "",
    val cost: String = "",
    val benefit: String = "",
    val note: String = "",
    val sources: List<Source> = emptyList(),
)

@Serializable
data class Source(
    val text: String = "",
    val url: String? = null,
)

/** 性价比三档，决定目录色点与徽章配色。 */
enum class Ratio(val label: String) {
    HIGHEST("极高"),
    HIGH("高"),
    NORMAL("一般");

    companion object {
        fun of(raw: String): Ratio = when (raw) {
            "极高" -> HIGHEST
            "高" -> HIGH
            else -> NORMAL
        }
    }
}

/** 顶部三档筛选，与网页版一致。 */
enum class FilterMode { ALL, GRADE_A, PLAIN_ONLY }

/** 搜索范围：全书 / 当前节。 */
enum class SearchScope { ALL, SECTION }

/** 多维筛选条件；null 表示该维度不限制。 */
data class FilterSpec(
    val grade: String? = null,        // A / B / C
    val ratio: String? = null,        // 极高 / 高 / 一般
    val caliber: String? = null,      // 口径：死亡率 / 时间 / 金钱 / 自由
    val money: String? = null,        // 钱：0 / 少 / 多
    val time: String? = null,         // 时间：少 / 中 / 多
    val grit: String? = null,         // 毅力：否 / 些 / 是
    val gain: String? = null,         // 收益：大 / 中 / 小
    val plainOnly: Boolean = false,   // 只说人话（展示模式）
    val scope: SearchScope = SearchScope.ALL,
) {
    /** 生效中的条件数（不含展示模式与范围）。 */
    val activeCount: Int
        get() = listOf(grade, ratio, caliber, money, time, grit, gain).count { it != null }

    val isDefault: Boolean get() = activeCount == 0 && !plainOnly

    fun matches(card: GuideCard): Boolean {
        if (grade != null && card.grade != grade) return false
        if (ratio != null && card.ratio != ratio) return false
        if (caliber != null && card.tags["口径"] != caliber) return false
        if (money != null && card.tags["钱"] != money) return false
        if (time != null && card.tags["时间"] != time) return false
        if (grit != null && card.tags["毅力"] != grit) return false
        if (gain != null && card.tags["收益"] != gain) return false
        return true
    }

    companion object {
        val GRADES = listOf("A", "B", "C")
        val RATIOS = listOf("极高", "高", "一般")
        val CALIBERS = listOf("死亡率", "时间", "金钱", "自由")
        val MONEY = listOf("0", "少", "多")
        val TIME = listOf("少", "中", "多")
        val GRIT = listOf("否", "些", "是")
        val GAIN = listOf("大", "中", "小")
    }
}
