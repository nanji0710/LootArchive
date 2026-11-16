package com.nanji.lootarchive.data.assistant

/**
 * 根据物品名称猜分类。只做常见词匹配，匹配不到时返回 null，交给调用方使用「其他」分类。
 * 目的：避免「平板」被塞进第一个分类「食品饮料」这种明显错误。
 */
object ItemCategoryGuesser {

    private val rules: List<Pair<String, List<String>>> = listOf(
        "数码电子" to listOf(
            "平板", "ipad", "tablet", "手机", "耳机", "电脑", "笔记本", "相机", "镜头",
            "充电", "充电器", "数据线", "键盘", "鼠标", "显示器", "音箱", "音响", "手表",
            "switch", "游戏机", "主机", "硬盘", "U盘", "路由器", "电视",
        ),
        "食品饮料" to listOf(
            "牛奶", "酸奶", "面包", "饼干", "蛋糕", "零食", "饮料", "咖啡", "茶叶", "茶",
            "酒", "水果", "苹果", "香蕉", "鸡蛋", "大米", "面条", "奶粉", "巧克力", "糖",
        ),
        "药品保健" to listOf(
            "药", "维生素", "保健品", "感冒", "退烧", "创可贴", "口罩", "体温计", "眼药水",
        ),
        "日用百货" to listOf(
            "纸巾", "洗发水", "沐浴露", "洗衣液", "牙膏", "牙刷", "毛巾", "清洁", "垃圾袋",
            "洗面奶", "护手霜", "沐浴", "肥皂",
        ),
        "服饰鞋包" to listOf(
            "衣服", "外套", "裤", "鞋", "运动鞋", "靴", "包", "背包", "帽子", "袜子", "围巾",
        ),
        "书籍文具" to listOf(
            "书", "教材", "笔记本本", "笔", "钢笔", "铅笔", "文具", "文件夹", "便签",
        ),
        "工具器材" to listOf(
            "工具", "螺丝", "螺丝刀", "锤", "扳手", "电钻", "钳", "胶带", "卷尺",
        ),
        "家居家具" to listOf(
            "桌", "椅子", "床", "柜", "沙发", "灯", "台灯", "枕头", "被子", "地毯",
        ),
        "藏品摆件" to listOf(
            "手办", "模型", "摆件", "收藏", "徽章", "海报", "纪念",
        ),
    )

    fun guess(name: String): String? {
        val text = name.lowercase()
        if (text.isBlank()) return null
        return rules.firstOrNull { (_, keywords) -> keywords.any { text.contains(it.lowercase()) } }?.first
    }
}
