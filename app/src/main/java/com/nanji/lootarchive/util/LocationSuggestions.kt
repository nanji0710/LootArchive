package com.nanji.lootarchive.util

/**
 * 内置存放位置建议（参考有数 app）
 */
object LocationSuggestions {

    /** 房间-位置 层级建议 */
    val suggestions: Map<String, List<String>> = linkedMapOf(
        "厨房" to listOf("冰箱", "橱柜", "台面", "调料架"),
        "客厅" to listOf("电视柜", "茶几", "书架", "收纳柜"),
        "卧室" to listOf("衣柜", "床头柜", "梳妆台", "床底收纳"),
        "卫生间" to listOf("洗手台", "储物柜", "淋浴间"),
        "阳台" to listOf("储物柜", "晾衣区"),
        "书房" to listOf("书桌", "书柜", "抽屉"),
        "储物间" to listOf("收纳架", "储物箱", "货架")
    )

    /** 扁平化为提示列表 */
    val flatList: List<String> by lazy {
        suggestions.flatMap { (room, locations) ->
            locations.map { "$room · $it" }
        }
    }
}
