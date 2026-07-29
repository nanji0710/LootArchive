package com.nanji.lootarchive.util

object ExpCalculator {
    /** Level: Pair(所需EXP, 等级名) */
    val LEVELS = listOf(
        0 to "入门",
        50 to "新手",
        150 to "爱好者",
        350 to "达人",
        600 to "收藏家",
        1000 to "专家",
        2000 to "大师",
        5000 to "藏家",
        10000 to "鉴赏家",
        20000 to "传奇"
    )

    fun getLevel(exp: Int): Int {
        var lv = 1
        for (i in LEVELS.indices) if (exp >= LEVELS[i].first) lv = i + 1
        return lv
    }

    fun getLevelTitle(level: Int): String = LEVELS.getOrElse(level - 1) { 0 to "入门" }.second

    fun getNextLevelExp(exp: Int): Int {
        val currentLevel = getLevel(exp)
        if (currentLevel >= LEVELS.size) return Int.MAX_VALUE
        return LEVELS[currentLevel].first
    }

    fun getLevelProgress(exp: Int): Float {
        val lv = getLevel(exp)
        if (lv >= LEVELS.size) return 1f
        val prev = LEVELS.getOrElse(lv - 1) { 0 to 0 }.first
        val next = LEVELS.getOrElse(lv) { 0 to 0 }.first
        if (next == prev) return 1f
        return ((exp - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    }

    object Rewards {
        const val ADD_ITEM = 5
        const val ADD_PHOTO = 2
        const val COMPLETE_DESCRIPTION = 3
        const val ITEM_COUNT_EXP = 10
        fun valueExp(price: Double): Int = (price / 1000).toInt()
    }
}
