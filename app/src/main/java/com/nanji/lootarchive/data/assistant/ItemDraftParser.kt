package com.nanji.lootarchive.data.assistant

import java.util.Calendar

/**
 * 「一句话新增物品」解析器。
 *
 * 用户对助理说例如：
 * 「新增物品：牛奶，价格 20 元，购买时间 2026年11月16日，保质期 30 天，放在冰箱」
 * 就解析出名称 / 价格 / 购买日期 / 保质期 / 到期日 / 分类提示 / 存放位置。
 *
 * 只在句子里出现明确的「新增/添加/买了/入手」等意图并且能提取到名称时返回结果，
 * 避免把普通问答误判成新增物品。
 */
data class ParsedItemDraft(
    val name: String,
    val price: Double? = null,
    val purchaseDate: Long? = null,
    val shelfLifeDays: Int? = null,
    val expiryDate: Long? = null,
    val categoryHint: String? = null,
    val location: String? = null,
    val note: String? = null,
) {
    val hasDetails: Boolean
        get() = price != null || purchaseDate != null || shelfLifeDays != null || expiryDate != null
}

object ItemDraftParser {

    private val INTENT = Regex("(新增|添加|帮我加|加一个|加个|记录一下|记录|记一下|买了|入手|购入)")
    private val NAME_LABEL = Regex("(?:名称|品名|物品名称)\\s*[：:]\\s*([^，,。；;\\n]+)")
    private val NAME_AFTER_INTENT = Regex(
        "(?:新增|添加|帮我加|加一个|加个|记录一下|记录|记一下|买了|入手|购入)(?:物品|东西)?[：:，,]?\\s*([^，,。；;\\n]+?)(?=(，|,|。|；|;|价格|单价|花费|花了|购买|买于|保质期|有效期|分类|类别|位置|放在|$))"
    )
    private val NAME_BUY = Regex("(?:买了|入手|购入)([^，,。；;\\d]+)")

    private val PRICE_LABEL = Regex("(?:价格|单价|花费|花了|售价)\\s*[：:]?\\s*[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)")
    private val PRICE_YUAN = Regex("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|块钱)?")
    private val PRICE_WITH_UNIT = Regex("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块钱|块)")

    private val DATE_FULL = Regex("(\\d{4})\\s*[年\\-/\\.]\\s*(\\d{1,2})\\s*[月\\-/\\.]\\s*(\\d{1,2})\\s*日?")
    private val DATE_EXPIRY_LABEL = Regex("(?:有效期至|有效期到|到期日|到期时间)\\s*[：:]?\\s*(\\d{4})\\s*[年\\-/\\.]\\s*(\\d{1,2})\\s*[月\\-/\\.]\\s*(\\d{1,2})\\s*日?")
    private val SHELF_LIFE = Regex("保质期(?:是|为)?\\s*[：:]?\\s*(\\d+)\\s*(天|日|个月|月|年)")
    private val CATEGORY_LABEL = Regex("(?:分类|类别|品类)\\s*[：:]\\s*([^，,。；;\\n]+)")
    private val LOCATION_LABEL = Regex("(?:位置|存放位置|存放|放在|收在)\\s*[：:]?\\s*([^，,。；;\\n]+)")
    private val NOTE_LABEL = Regex("(?:备注|说明)\\s*[：:]\\s*([^，,。；;\\n]+)")

    fun parse(raw: String): ParsedItemDraft? {
        val text = raw.trim()
        if (text.isEmpty() || !INTENT.containsMatchIn(text)) return null

        val name = (NAME_LABEL.find(text)?.groupValues?.get(1)
            ?: NAME_AFTER_INTENT.find(text)?.groupValues?.get(1)
            ?: NAME_BUY.find(text)?.groupValues?.get(1))
            ?.trim()
            ?.trim('。', '，', ',', '；', ';', ' ')
            ?.removePrefix("一个")
            ?.removePrefix("一件")
            ?.removePrefix("物品")
            ?.removePrefix("东西")
            ?.trim()
            ?.replace(Regex("\\s*\\d+(?:\\.\\d{1,2})?\\s*(?:元|块钱|块)\\s*$"), "")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length <= 40 }
            ?: return null

        // 价格：优先「价格/单价/花费」标签，其次 ¥ 符号，最后「xx 元」
        val price = PRICE_LABEL.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: PRICE_YUAN.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: PRICE_WITH_UNIT.find(text)?.groupValues?.get(1)?.toDoubleOrNull()

        // 购买日期：明确日期 > 今天/昨天/前天；未说就默认今天（新增物品的常见默认）
        val now = System.currentTimeMillis()
        val explicitPurchase = parseDate(DATE_FULL.find(text)?.value)
        val purchaseDate = explicitPurchase
            ?: when {
                text.contains("前天") -> now - 2 * DAY_MS
                text.contains("昨天") -> now - DAY_MS
                else -> now
            }

        // 保质期 / 有效期
        val shelfLifeDays = SHELF_LIFE.find(text)?.let { m ->
            val amount = m.groupValues[1].toIntOrNull() ?: return@let null
            when (m.groupValues[2]) {
                "个月", "月" -> amount * 30
                "年" -> amount * 365
                else -> amount
            }
        }
        val explicitExpiry = parseDate(DATE_EXPIRY_LABEL.find(text)?.value)
        val expiryDate = explicitExpiry ?: shelfLifeDays?.let { purchaseDate + it * DAY_MS }

        val categoryHint = CATEGORY_LABEL.find(text)?.groupValues?.get(1)?.trim()
        val location = LOCATION_LABEL.find(text)?.groupValues?.get(1)?.trim()
        val note = NOTE_LABEL.find(text)?.groupValues?.get(1)?.trim()

        // 防误判：像「添加的物品有哪些？」这类问句，若没有任何价格/日期/保质期细节，
        // 不当作新增操作，交给正常问答。
        val looksLikeQuestion = text.contains("？") || text.contains("?") ||
            listOf("哪些", "什么", "多少", "怎么", "吗").any { text.contains(it) }
        val hasConcreteDetail = price != null || explicitPurchase != null || shelfLifeDays != null || explicitExpiry != null
        if (looksLikeQuestion && !hasConcreteDetail) return null

        return ParsedItemDraft(
            name = name,
            price = price,
            purchaseDate = purchaseDate,
            shelfLifeDays = shelfLifeDays,
            expiryDate = expiryDate,
            categoryHint = categoryHint,
            location = location,
            note = note,
        )
    }

    /**
     * 解析「修改」类指令，例如：
     * - 名字写错了，是平板
     * - 把名字改成 iPad
     * - 价格改成 2599
     * - 分类改成数码电子
     *
     * 返回的字段里只有用户明确提到的项才会非空；name 为空表示只改分类/价格等。
     */
    fun parseEdits(raw: String): ParsedItemDraft? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (listOf("改", "换", "不是", "错", "修正").none { text.contains(it) }) return null

        val categoryOnly = (text.contains("分类") || text.contains("类别")) &&
            !text.contains("名字") && !text.contains("名称")
        val name = if (categoryOnly) {
            null
        } else {
            val candidates = listOf(
                Regex("(?:名字|名称)\\s*[：:]\\s*([^，,。；;\\n]+)"),
                Regex("(?:名字|名称)[^，,。；;]*(?:改成|改为|换成|写成|是|叫)\\s*[：:]?\\s*([^，,。；;\\n]+)"),
                Regex("(?:是|叫)\\s*[：:]?\\s*([^，,。；;\\n]+)"),
                Regex("(?:改成|改为|换成|写成)\\s*[：:]?\\s*([^，,。；;\\n]+)"),
                Regex("(?:名字|名称)\\s+([^，,。；;\\n]+)"),
                Regex("(?:名字|名称)[：:]?\\s*([^，,。；;\\n]+)"),
            )
            candidates.asSequence()
                .mapNotNull { it.find(text)?.groupValues?.get(1)?.trim() }
                .map { it.trimEnd('呀', '啊', '。', '，', ',') }
                .firstOrNull { value ->
                    value.length <= 40 &&
                        !value.contains("价") && !value.contains("元") &&
                        !value.contains("分类") && !value.contains("类别") &&
                        !value.contains("错") &&
                        !Regex("(\\d|年|月|日|号|今年|去年|明年)").containsMatchIn(value)
                }
        }
        val price = PRICE_LABEL.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: PRICE_YUAN.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: PRICE_WITH_UNIT.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val shelfLifeDays = SHELF_LIFE.find(text)?.let { m ->
            val amount = m.groupValues[1].toIntOrNull() ?: return@let null
            when (m.groupValues[2]) {
                "个月", "月" -> amount * 30
                "年" -> amount * 365
                else -> amount
            }
        }
        val categoryHint = CATEGORY_LABEL.find(text)?.groupValues?.get(1)?.trim()
        val location = LOCATION_LABEL.find(text)?.groupValues?.get(1)?.trim()
        val note = NOTE_LABEL.find(text)?.groupValues?.get(1)?.trim()
        if (name.isNullOrBlank() && price == null && shelfLifeDays == null &&
            categoryHint == null && location == null && note == null
        ) return null
        return ParsedItemDraft(
            name = name.orEmpty(),
            price = price,
            shelfLifeDays = shelfLifeDays,
            categoryHint = categoryHint,
            location = location,
            note = note,
        )
    }

    private fun parseDate(segment: String?): Long? {
        if (segment.isNullOrBlank()) return null
        val m = DATE_FULL.find(segment) ?: return null
        val (y, mo, d) = m.destructured
        return runCatching {
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(y.toInt(), mo.toInt() - 1, d.toInt(), 12, 0, 0)
            cal.timeInMillis
        }.getOrNull()
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
