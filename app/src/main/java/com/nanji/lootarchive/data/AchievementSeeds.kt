package com.nanji.lootarchive.data

/**
 * 内置 13 枚成就徽章定义 — 单源维护（DB seed / UI 展示共用，避免两份定义漂移）。
 */
object AchievementSeeds {
    data class Seed(
        val key: String,
        val title: String,
        val description: String,
        val category: String,
        val target: Int
    )

    val all = listOf(
        Seed("items_5", "初级收藏", "收集5件物品", "collection", 5),
        Seed("items_20", "中级收藏家", "收集20件物品", "collection", 20),
        Seed("items_50", "高级收藏家", "收集50件物品", "collection", 50),
        Seed("items_100", "百物之主", "收集100件物品", "collection", 100),
        Seed("value_10000", "万元户", "总资产超过1万", "value", 10000),
        Seed("value_100000", "小富翁", "总资产超过10万", "value", 100000),
        Seed("value_500000", "财富自由", "总资产超过50万", "value", 500000),
        Seed("photos_10", "随手拍", "拍摄10张照片", "photo", 10),
        Seed("photos_50", "摄影师", "拍摄50张照片", "photo", 50),
        Seed("desc_10", "细节控", "完善10件物品描述", "detail", 10),
        Seed("desc_50", "文字家", "完善50件物品描述", "detail", 50),
        Seed("streak_7", "坚持一周", "连续7天活跃", "streak", 7),
        Seed("streak_30", "月常打卡", "连续30天活跃", "streak", 30)
    )
}
