package com.nanji.lootarchive.data

/**
 * 内置 10 大默认分类 — 单源维护（DB seed / 首次启动补种共用，避免两份定义漂移）。
 */
object DefaultCategories {
    val all = listOf(
        "食品饮料" to "restaurant",
        "药品保健" to "medical_services",
        "日用百货" to "local_mall",
        "数码电子" to "smartphone",
        "服饰鞋包" to "checkroom",
        "书籍文具" to "menu_book",
        "工具器材" to "build",
        "藏品摆件" to "diamond",
        "家居家具" to "chair",
        "其他" to "category"
    )
}
