package com.nanji.lootarchive.data.assistant

import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.util.DateUtil
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
//  本地问答引擎 —— 离线也能用的「物品管家」
//
//  拾物集是纯本地 App，助理在**完全没有网络配置**时也必须是可用的，
//  所以「听懂问题 + 算出答案」这件事只能用本地规则完成：
//  用户的一句话 → 意图 → 基于 Room 里真实数据的结构化回答。
//  远程模型（RemoteAssistant）只是可选的一层润色，不承担基础能力。
//
//  为什么不用模型做意图识别：
//  1) 离线可用是产品前提，不能把「我的东西值多少钱」押在网络上；
//  2) 件数/金额由本地算可复现、可核对，不会出现模型算错账；
//  3) 回答里要带**可点击的物品条目**（跳详情页），这需要真实的 ItemEntity。
// ═══════════════════════════════════════════════════════════════

/**
 * 回答里的一块结构化内容。
 *
 * [lines] 的语义随 [kind] 变化：
 * - [Kind.SUMMARY]：`标签|数值` 用竖线分隔，界面拆成两列小指标（没有竖线则当作纯标签）；
 * - [Kind.LIST]：通常为空，内容看 [items]；
 * - [Kind.TIP] / [Kind.TEXT] / [Kind.UNKNOWN]：每行一句。
 */
data class AnswerBlock(
    val kind: Kind,
    val title: String,
    val lines: List<String>,
    val items: List<ItemEntity> = emptyList()
) {
    enum class Kind { SUMMARY, LIST, TIP, TEXT, UNKNOWN }
}

/** 一次回答：一句口语化的话 + 结构化块 + 可追问的建议。 */
data class AssistantAnswer(
    val text: String,
    val blocks: List<AnswerBlock> = emptyList(),
    val suggestions: List<String> = emptyList(),
    /** 远端模型返回的思考过程；本地规则回答为空。 */
    val reasoning: String = "",
    /** 是否由远端模型生成。 */
    val fromModel: Boolean = false,
)

// ── 引擎内部常量 ──
//
// 放在文件顶层而不是 companion：Snapshot 的默认参数也要用到默认保修窗口，
// 嵌套类里引用外层 companion 需要写全限定名，反而更绕。

/** 设置里读不到保修提醒天数时的兜底窗口。 */
private const val DEFAULT_WARRANTY_DAYS = 30
private const val DAY_MS = 24L * 60 * 60 * 1000

/** 列表类回答最多列几件（再多就该去首页/搜索页翻）。 */
private const val LIST_LIMIT = 6

/** 发给远程模型的最多物品条数：控制请求体大小，避免几千件物品把上下文撑爆。 */
private const val MAX_REMOTE_ITEMS = 60

/** 「XX 在哪」里物品名的最大长度：再长就当成整句话，不当作物品名。 */
private const val MAX_SEARCH_TERM = 12

private const val UNKNOWN_LOCATION = "未填写位置"
private const val UNKNOWN_CATEGORY = "未分类"

// ── 意图关键词表 ──
//
// 这里收的是中文口语变体，不追求穷举，只覆盖「用户真的会怎么问」的高频说法；
// 漏掉的问法会落到兜底回答（说明能力 + 推荐问法），不会给出错误数据。

private val GREETING_KEYS = arrayOf("你好", "您好", "嗨", "hi", "hello", "在吗", "在么")

private val HELP_KEYS = arrayOf(
    "你能做什么", "你会什么", "能干什么", "能帮我做什么", "有什么功能",
    "怎么用", "你是谁", "你是什么", "介绍一下你", "帮助", "会干嘛"
)

/** 显式搜索前缀：`搜索耳机` / `帮我找一下充电器`。 */
private val SEARCH_PREFIXES = arrayOf("搜索", "搜一下", "搜搜", "查找", "找一下", "找找", "查一下", "查查", "帮我找", "帮我查")

/** 搜索词里要剥掉的客套/指代。 */
private val SEARCH_FILLERS = arrayOf("一下", "我的", "帮我", "那个", "这件", "那件", "这个", "东西", "物品", "的", "有哪些", "有没有", "吗", "呢", "？", "?")

/** 「X 在哪」的疑问标记。取最早出现的那个，前面那段当作物品名。 */
private val WHERE_MARKERS = arrayOf(
    "在哪里", "在哪儿", "在哪", "哪里", "哪儿", "放在什么地方", "放在什么位置",
    "放在哪", "放哪了", "放哪", "收在哪", "搁哪", "搁在"
)

/** 问句前半段里要剥掉的填充词。 */
private val WHERE_FILLERS = arrayOf(
    "请问", "帮我查", "帮我", "我想知道", "我想问", "查一下", "找一下", "看看",
    "那个", "这件", "那件", "这个", "我的", "我", "的", "都", "又", "上次", "之前", "前几天", "新买", "买的",
    // 「最近添加的耳机在哪」应该按「耳机」去搜，而不是拿整句去匹配名称
    "最近添加", "最近买", "刚添加", "新添加"
)

/** 指代整类东西的词：它们出现在「在哪」前面时是在问位置分布，而不是找某一件。 */
private val GENERIC_TERMS = setOf(
    "东西", "物品", "这些", "那些", "它们", "家当", "宝贝", "所有东西", "全部", "全部东西", "那个", "这件", "那件"
)

private val PUNCT_REGEX = Regex("[\\p{P}\\p{S}\\s]")

/** 能力词：显式搜索的剩余词如果本身就是能力词（如「查一下保修」），让位给意图识别。 */
private val INTENT_WORDS = arrayOf(
    "保修", "质保", "到期", "分类", "类别", "统计", "资产", "价值", "多少钱", "闲置",
    "待修", "建议", "省钱", "存放", "位置", "最近", "最贵", "最便宜", "丢失", "已出", "在用"
)

private val LOCATION_KEYS = arrayOf("存放", "收纳", "位置", "放哪", "放在哪", "搁哪", "收在哪", "放哪里")
private val WARRANTY_KEYS = arrayOf("保修", "质保", "到期", "过期", "过保", "售后")
private val RECENT_KEYS = arrayOf("最近", "最新", "刚买", "新买", "新添加", "刚添加", "最近买", "最近加")
private val CATEGORY_KEYS = arrayOf("分类", "类别", "品类", "哪类", "分类分布", "分类占比")
private val IDLE_KEYS = arrayOf("闲置", "吃灰", "积灰", "没用过", "不用了")
private val REPAIR_KEYS = arrayOf("待修", "维修", "坏了", "要修", "需要修", "修理")
private val SOLD_KEYS = arrayOf("已出", "卖掉", "卖了", "出手", "转让", "已出售")
private val LOST_KEYS = arrayOf("丢失", "丢了", "找不到", "遗失", "不见了")
private val ACTIVE_KEYS = arrayOf("在用", "还在用", "正常用", "活跃")
private val EXPENSIVE_KEYS = arrayOf("最贵", "最值钱", "最烧钱", "单价最高", "值钱")
private val CHEAP_KEYS = arrayOf("最便宜", "最省钱", "最不值钱", "价格最低")
private val VALUE_KEYS = arrayOf(
    "总价值", "总值", "总金额", "总额", "总资产", "资产", "合计", "花了多少", "一共多少钱",
    "总共多少钱", "值多少钱", "值多少", "一共花", "总共花"
)
private val COUNT_KEYS = arrayOf("多少件", "几件", "多少个", "数量", "统计", "多少样", "件数", "一共多少", "总共多少")
private val ADVICE_KEYS = arrayOf("省钱", "建议", "优化", "怎么管理", "管理建议", "如何管理", "减少浪费", "怎么整理", "有什么办法")

private val DEFAULT_SUGGESTIONS = listOf("我的东西一共值多少钱？", "有哪些保修快到期了？", "最近添加了什么？", "新增物品：牛奶，价格20元，保质期30天")

/**
 * 本地问答引擎（无状态，可安全地在任意线程调用）。
 *
 * 输入：用户的一句话 + [Snapshot] 数据快照；输出：[AssistantAnswer]。
 * 引擎本身不碰数据库、不碰网络 —— 数据由 ViewModel 组合好后喂进来，
 * 这样规则部分可以脱离 Android 环境单独推理与测试。
 */
class AssistantEngine {

    /**
     * 一次问答所需的数据快照，由 ViewModel 从三个 Repository 组合而来。
     *
     * [photoItemIds] 只保存物品 id，**不保存照片路径**：建议意图只需要知道
     * 「哪几件没有照片」，路径属于设备隐私，任何一层都不该往后传。
     */
    data class Snapshot(
        val items: List<ItemEntity> = emptyList(),
        val categories: List<CategoryEntity> = emptyList(),
        /** 保修提前提醒天数（设置页可调）；读不到时用 30 天并在文案里写明。 */
        val warrantyReminderDays: Int = DEFAULT_WARRANTY_DAYS,
        val photoItemIds: Set<Long> = emptySet(),
        val now: Long = System.currentTimeMillis()
    )

    /** 用户问题 → 结构化回答。输入相同、快照相同，结果必然相同。 */
    fun answer(question: String, snapshot: Snapshot): AssistantAnswer {
        val query = classify(question)
        // 空库时不返回一堆 0，直接给「怎么开始」——比「0 件物品，合计 ¥0」有用得多
        if (snapshot.items.isEmpty() && query !is Query.Help) return emptyLibraryAnswer()
        return when (query) {
            Query.Help -> helpAnswer(snapshot, understood = true)
            Query.Greeting -> greetingAnswer(snapshot)
            Query.Unknown -> helpAnswer(snapshot, understood = false)
            is Query.Search -> searchAnswer(query.term, snapshot)
            Query.Location -> locationAnswer(snapshot)
            Query.Warranty -> warrantyAnswer(snapshot)
            Query.Recent -> recentAnswer(snapshot)
            Query.Category -> categoryAnswer(snapshot)
            Query.Expensive -> priceAnswer(snapshot, expensive = true)
            Query.Cheapest -> priceAnswer(snapshot, expensive = false)
            is Query.ByStatus -> statusAnswer(query.status, snapshot)
            Query.Value -> valueAnswer(snapshot)
            Query.Count -> countAnswer(snapshot)
            Query.Advice -> adviceAnswer(snapshot)
        }
    }

    /**
     * 生成给远程模型看的「紧凑库存摘要」（JSON）。
     *
     * 安全边界写死在这里，调用点没有机会塞进别的字段：
     * 只导出 名称 / 分类 / 价格 / 状态 / 购买日期 / 保修到期 / 存放位置 / 标签；
     * **绝不导出照片路径、备注、数据库 id** ——
     * 照片路径是用户设备上的绝对路径，备注里可能写着购买渠道、序列号等隐私信息。
     */
    fun buildInventoryDigest(snapshot: Snapshot): String {
        val exported = snapshot.items.take(MAX_REMOTE_ITEMS)
        val root = JSONObject()
        root.put("totalItems", snapshot.items.size)
        root.put("ownedValue", round2(ownedValue(snapshot.items)))
        val categoryArray = JSONArray()
        snapshot.categories.forEach { categoryArray.put(it.name) }
        root.put("categories", categoryArray)
        val itemArray = JSONArray()
        exported.forEach { item ->
            val json = JSONObject()
            json.put("name", item.name)
            json.put("category", categoryName(snapshot, item.categoryId))
            json.put("price", round2(item.purchasePrice))
            json.put("status", ItemStatus.fromCode(item.status).label)
            json.put("purchased", item.purchaseDate?.let { DateUtil.formatTimestamp(it) } ?: "")
            json.put("warranty", item.warrantyExpiryDate?.let { DateUtil.formatTimestamp(it) } ?: "")
            json.put("location", item.storageLocation)
            val tagArray = JSONArray()
            item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tagArray.put(it) }
            json.put("tags", tagArray)
            itemArray.put(json)
        }
        root.put("items", itemArray)
        // 物品太多时只发前 N 件，并显式告诉模型「这是截断的」，避免它据此下结论说「你只有 60 件」
        if (snapshot.items.size > exported.size) root.put("truncated", true)
        return root.toString()
    }

    // ══════════════════════════════════════════════════════════
    //  意图识别
    // ══════════════════════════════════════════════════════════

    /** 意图识别结果：每个分支对应一个回答构造函数。 */
    private sealed interface Query {
        data object Greeting : Query
        data object Help : Query
        data object Unknown : Query
        data class Search(val term: String) : Query
        data object Location : Query
        data object Warranty : Query
        data object Recent : Query
        data object Category : Query
        data object Expensive : Query
        data object Cheapest : Query
        data class ByStatus(val status: ItemStatus) : Query
        data object Value : Query
        data object Count : Query
        data object Advice : Query
    }

    /**
     * 关键词 + 规则识别意图。
     *
     * 顺序本身就是优先级，几条容易打架的规则按下面这些实际问法排过：
     * - 「哪个分类最贵」→ 分类（分类规则排在最贵之前）；
     * - 「最近有什么保修到期」→ 保修（保修排在最近之前）；
     * - 「有多少件闲置」→ 闲置状态（状态排在件数之前）；
     * - 「查一下保修」→ 保修（显式搜索词是能力词时让位）。
     */
    private fun classify(question: String): Query {
        val text = question.lowercase(Locale.getDefault()).replace(Regex("\\s+"), "").trim()
        if (text.isEmpty()) return Query.Help

        // 打招呼只在整句很短时才认：「hi」这类拉丁字母也可能是英文名/型号的一部分
        if (text.length <= 6 && text.hasAny(*GREETING_KEYS)) return Query.Greeting
        if (text.hasAny(*HELP_KEYS)) return Query.Help

        // 显式搜索：「搜索耳机」「帮我找一下充电器」
        SEARCH_PREFIXES.forEach { prefix ->
            val index = text.indexOf(prefix)
            if (index >= 0) {
                val term = cleanSearchTerm(text.substring(index + prefix.length))
                if (term.isNotEmpty() && !term.hasAny(*INTENT_WORDS)) return Query.Search(term)
            }
        }

        // 「我的耳机在哪」「充电器放哪了」这类问法：把问句前半段当成物品名/标签
        extractWhereTerm(text)?.let { return Query.Search(it) }

        if (text.hasAny(*LOCATION_KEYS)) return Query.Location
        if (text.hasAny(*WARRANTY_KEYS)) return Query.Warranty
        if (text.hasAny(*RECENT_KEYS)) return Query.Recent
        if (text.hasAny(*CATEGORY_KEYS)) return Query.Category
        statusOf(text)?.let { return Query.ByStatus(it) }
        if (text.hasAny(*EXPENSIVE_KEYS)) return Query.Expensive
        if (text.hasAny(*CHEAP_KEYS)) return Query.Cheapest
        if (text.hasAny(*VALUE_KEYS)) return Query.Value
        if (text.hasAny(*COUNT_KEYS)) return Query.Count
        if (text.hasAny(*ADVICE_KEYS)) return Query.Advice
        return Query.Unknown
    }

    /** 字符串里是否命中任意关键词。 */
    private fun String.hasAny(vararg keys: String): Boolean = keys.any { contains(it, ignoreCase = true) }

    /** 剥掉搜索词里的客套与指代，「搜索我的耳机」→「耳机」。 */
    private fun cleanSearchTerm(raw: String): String {
        var term = raw
        SEARCH_FILLERS.forEach { term = term.replace(it, "") }
        return term.trim()
    }

    /**
     * 从「X 在哪」里抽出 X。
     *
     * 只有前半段是一个具体名词时才返回（「东西放哪了」里的「东西」是指代整类，
     * 应该走位置分布；「我的耳机在哪」里的「耳机」才是要找的那件东西）。
     */
    private fun extractWhereTerm(text: String): String? {
        val markerIndex = WHERE_MARKERS.map { text.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return null
        var term = text.substring(0, markerIndex).replace(PUNCT_REGEX, "")
        WHERE_FILLERS.forEach { term = term.replace(it, "") }
        term = term.trim()
        if (term.isEmpty() || term.length > MAX_SEARCH_TERM || term in GENERIC_TERMS) return null
        return term
    }

    /** 状态类问法（闲置/待修/已出/丢失/在用）。 */
    private fun statusOf(text: String): ItemStatus? = when {
        text.hasAny(*IDLE_KEYS) -> ItemStatus.IDLE
        text.hasAny(*REPAIR_KEYS) -> ItemStatus.REPAIR
        text.hasAny(*SOLD_KEYS) -> ItemStatus.SOLD
        text.hasAny(*LOST_KEYS) -> ItemStatus.LOST
        text.hasAny(*ACTIVE_KEYS) -> ItemStatus.ACTIVE
        else -> null
    }

    // ══════════════════════════════════════════════════════════
    //  各意图的回答
    // ══════════════════════════════════════════════════════════

    /** 空库：所有问题都先引导「怎么开始记录」。 */
    private fun emptyLibraryAnswer(): AssistantAnswer = AssistantAnswer(
        text = "你的物品库还是空的，我暂时没有可以分析的数据。先在首页记下第一件物品，之后就能问我「我的东西值多少钱」「有哪些保修快到期了」。",
        blocks = listOf(
            AnswerBlock(
                kind = AnswerBlock.Kind.TIP,
                title = "开始之前，先记这三样",
                lines = listOf(
                    "① 名称 + 价格 + 分类：统计和排行都靠这三项。",
                    "② 存放位置：以后问「充电器放哪了」，我能直接回答。",
                    "③ 保修到期日：到期前会提醒你。"
                )
            )
        ),
        suggestions = listOf("怎么记录物品？", "保修提醒有什么用？", "统计页能看什么？")
    )

    /** 打招呼：先自我介绍，再给能力清单与推荐问法。 */
    private fun greetingAnswer(snapshot: Snapshot): AssistantAnswer = AssistantAnswer(
        text = "你好呀，我是拾物助手。现在库里有 ${count(snapshot.items.size)} 件物品，所有计算都在本机完成，想从哪问起？",
        blocks = listOf(capabilityBlock()),
        suggestions = DEFAULT_SUGGESTIONS
    )

    /**
     * 能力说明 / 兜底。
     *
     * @param understood true = 用户明确问「你能做什么」；false = 没识别出意图
     */
    private fun helpAnswer(snapshot: Snapshot, understood: Boolean): AssistantAnswer {
        val intro = if (understood) {
            "我是拾物助手，能直接用你本地的物品数据回答问题。"
        } else {
            "这句话我还没太听懂，不过下面这些我都在行："
        }
        val text = buildString {
            append(intro)
            if (snapshot.items.isNotEmpty()) {
                append("现在记录了 ${count(snapshot.items.size)} 件物品。")
            }
        }
        return AssistantAnswer(
            text = text,
            blocks = listOf(capabilityBlock()),
            suggestions = DEFAULT_SUGGESTIONS
        )
    }

    /** 能力清单（兜底与问候共用）。 */
    private fun capabilityBlock(): AnswerBlock = AnswerBlock(
        kind = AnswerBlock.Kind.UNKNOWN,
        title = "我能帮你做什么",
        lines = listOf(
            "• 算钱：我的东西一共值多少钱",
            "• 找东西：我的耳机在哪",
            "• 盘点：有多少件 / 分类分布 / 最贵的 3 件",
            "• 提醒：有哪些保修快到期了",
            "• 建议：怎么省钱、怎么整理",
            "• 记账：说「新增物品：牛奶，价格20元，购买日期2026-11-16，保质期30天」我直接帮你录入"
        )
    )

    /** 总价值：合计金额 + 在用/闲置拆解。 */
    private fun valueAnswer(snapshot: Snapshot): AssistantAnswer {
        val items = snapshot.items
        val owned = items.filter { ItemStatus.fromCode(it.status).isOwned }
        val ownedValue = owned.sumOf { it.purchasePrice }
        val allValue = items.sumOf { it.purchasePrice }
        val counts = statusCounts(items)
        val soldLost = (counts[ItemStatus.SOLD] ?: 0) + (counts[ItemStatus.LOST] ?: 0)
        return AssistantAnswer(
            text = "你现在一共 ${count(items.size)} 件物品，在持 ${count(owned.size)} 件，合计约 ${money(ownedValue)}。",
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.SUMMARY,
                    title = "资产总览",
                    lines = listOf(
                        "在持金额|${money(ownedValue)}",
                        "全部购入|${money(allValue)}",
                        "在用|${count(counts[ItemStatus.ACTIVE] ?: 0)} 件",
                        "闲置|${count(counts[ItemStatus.IDLE] ?: 0)} 件",
                        "待修|${count(counts[ItemStatus.REPAIR] ?: 0)} 件",
                        "已出/丢失|${count(soldLost)} 件"
                    )
                )
            ),
            suggestions = listOf("哪个分类最贵？", "最贵的 3 件是什么？", "保修快到期了吗？")
        )
    }

    /** 数量与状态分布。 */
    private fun countAnswer(snapshot: Snapshot): AssistantAnswer {
        val items = snapshot.items
        val counts = statusCounts(items)
        // 枚举声明顺序即「在用 → 闲置 → 已出 → 待修 → 丢失」，与 ItemStatus 里的定义保持一致
        val lines = ItemStatus.entries.map { status -> "${status.label}|${count(counts[status] ?: 0)} 件" } +
            "合计|${count(items.size)} 件"
        return AssistantAnswer(
            text = "一共 ${count(items.size)} 件物品，其中在用 ${count(counts[ItemStatus.ACTIVE] ?: 0)} 件、闲置 ${count(counts[ItemStatus.IDLE] ?: 0)} 件、待修 ${count(counts[ItemStatus.REPAIR] ?: 0)} 件。",
            blocks = listOf(AnswerBlock(AnswerBlock.Kind.SUMMARY, "状态分布", lines)),
            suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？", "有哪些闲置了？")
        )
    }

    /** 分类分布：件数 + 金额，按金额降序取前 6。 */
    private fun categoryAnswer(snapshot: Snapshot): AssistantAnswer {
        val groups = snapshot.items
            .groupBy { it.categoryId }
            .map { (categoryId, list) ->
                CategoryGroup(
                    name = categoryName(snapshot, categoryId),
                    count = list.size,
                    value = list.sumOf { it.purchasePrice }
                )
            }
            .sortedWith(compareByDescending<CategoryGroup> { it.value }.thenByDescending { it.count })
        val lines = buildList {
            groups.take(LIST_LIMIT).forEach { group ->
                add("${group.name}：${count(group.count)} 件 · ${money(group.value)}")
            }
            if (groups.size > LIST_LIMIT) add("…另有 ${count(groups.size - LIST_LIMIT)} 个分类未列出")
        }
        val top = groups.first()
        return AssistantAnswer(
            text = "一共 ${count(groups.size)} 个分类，金额最高的是「${top.name}」：${count(top.count)} 件 · ${money(top.value)}。",
            blocks = listOf(AnswerBlock(AnswerBlock.Kind.TEXT, "分类分布（按金额排序）", lines)),
            suggestions = listOf("最贵的 3 件是什么？", "我的东西一共值多少钱？", "有哪些闲置了？")
        )
    }

    /** 保修提醒：按设置里的提前天数筛，列出明细（可点进详情）。 */
    private fun warrantyAnswer(snapshot: Snapshot): AssistantAnswer {
        val days = snapshot.warrantyReminderDays.coerceIn(1, 365)
        val threshold = snapshot.now + days * DAY_MS
        val expiring = snapshot.items
            .filter { item ->
                val expiry = item.warrantyExpiryDate ?: return@filter false
                // 与 ItemDao.getWarrantyExpiringItems 同口径：已出的物品不再提醒
                ItemStatus.fromCode(item.status) != ItemStatus.SOLD && expiry <= threshold
            }
            .sortedBy { it.warrantyExpiryDate ?: Long.MAX_VALUE }
        if (expiring.isEmpty()) {
            return AssistantAnswer(
                text = "按设置里的「提前 $days 天」提醒，近 $days 天内没有保修到期的物品，挺省心的。",
                blocks = listOf(
                    AnswerBlock(
                        kind = AnswerBlock.Kind.TIP,
                        title = "保修信息在哪",
                        lines = listOf("在物品详情页可以补填购买日期与保修天数，填了才会进入提醒。")
                    )
                ),
                suggestions = listOf("有哪些闲置了？", "怎么省钱？", "我的东西一共值多少钱？")
            )
        }
        val expired = expiring.count { (it.warrantyExpiryDate ?: snapshot.now) < snapshot.now }
        val first = expiring.first()
        val firstExpiry = first.warrantyExpiryDate ?: snapshot.now
        val text = buildString {
            append("按设置里的「提前 $days 天」提醒，有 ${count(expiring.size)} 件物品的保修要留意")
            if (expired > 0) append("（其中 $expired 件已过期）")
            append("。最近的是「${first.name}」，${DateUtil.formatRemainingDays(firstExpiry)}。")
        }
        return AssistantAnswer(
            text = text,
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.LIST,
                    title = "保修提醒（$days 天窗口）",
                    lines = emptyList(),
                    items = expiring.take(LIST_LIMIT)
                )
            ),
            suggestions = listOf("有哪些闲置了？", "最近添加了什么？", "怎么省钱？")
        )
    }

    /** 最近添加。 */
    private fun recentAnswer(snapshot: Snapshot): AssistantAnswer {
        val recent = snapshot.items.sortedByDescending { it.createdAt }.take(5)
        return AssistantAnswer(
            text = "最近记录的 ${count(recent.size)} 件物品，最新一件是「${recent.first().name}」。",
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.LIST,
                    title = "最近添加（按添加时间倒序）",
                    lines = emptyList(),
                    items = recent
                )
            ),
            suggestions = listOf("我的东西一共值多少钱？", "有哪些保修快到期了？", "最贵的 3 件是什么？")
        )
    }

    /** 最贵 / 最便宜 Top 3。 */
    private fun priceAnswer(snapshot: Snapshot, expensive: Boolean): AssistantAnswer {
        val priced = snapshot.items.filter { it.purchasePrice > 0.0 }
        if (priced.isEmpty()) {
            return AssistantAnswer(
                text = "还没有填写价格的物品，补上价格之后我才能排出最贵和最便宜。",
                blocks = emptyList(),
                suggestions = DEFAULT_SUGGESTIONS
            )
        }
        val sorted = if (expensive) {
            priced.sortedByDescending { it.purchasePrice }
        } else {
            priced.sortedBy { it.purchasePrice }
        }
        val top = sorted.take(3)
        val total = top.sumOf { it.purchasePrice }
        val text = if (expensive) {
            "最贵的是「${top.first().name}」（${money(top.first().purchasePrice)}），前 3 件合计 ${money(total)}。"
        } else {
            "最便宜的是「${top.first().name}」（${money(top.first().purchasePrice)}），前 3 件合计 ${money(total)}。"
        }
        return AssistantAnswer(
            text = text,
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.LIST,
                    title = if (expensive) "最贵的 3 件" else "最便宜的 3 件",
                    lines = emptyList(),
                    items = top
                )
            ),
            suggestions = listOf("哪个分类最贵？", "我的东西一共值多少钱？", "有哪些保修快到期了？")
        )
    }

    /** 按状态列物品（闲置/待修/已出/丢失/在用）。 */
    private fun statusAnswer(status: ItemStatus, snapshot: Snapshot): AssistantAnswer {
        val label = status.label
        val matched = snapshot.items
            .filter { ItemStatus.fromCode(it.status) == status }
            .sortedByDescending { it.updatedAt }
        if (matched.isEmpty()) {
            return AssistantAnswer(
                text = "现在没有标记为「$label」的物品。",
                blocks = listOf(
                    AnswerBlock(
                        kind = AnswerBlock.Kind.TEXT,
                        title = "状态：$label",
                        lines = listOf("在物品详情页把状态改成「$label」，之后我就能按状态帮你统计。")
                    )
                ),
                suggestions = DEFAULT_SUGGESTIONS
            )
        }
        val total = matched.sumOf { it.purchasePrice }
        val hint = when (status) {
            ItemStatus.IDLE -> "长期吃灰的话，可以考虑出掉或送给需要的人。"
            ItemStatus.REPAIR -> "修好记得把状态改回「在用」。"
            ItemStatus.SOLD -> "已出的物品不计入在持资产。"
            ItemStatus.LOST -> "如果东西其实还在、只是忘了放哪，可以问我「某某在哪」。"
            ItemStatus.ACTIVE -> "这些都是你正在用的。"
        }
        return AssistantAnswer(
            text = "有 ${count(matched.size)} 件「$label」物品，合计 ${money(total)}。$hint",
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.LIST,
                    title = "「$label」${count(matched.size)} 件",
                    lines = emptyList(),
                    items = matched.take(LIST_LIMIT)
                )
            ),
            suggestions = listOf("我的东西一共值多少钱？", "有哪些保修快到期了？", "怎么省钱？")
        )
    }

    /** 存放位置分布：按件数聚合。 */
    private fun locationAnswer(snapshot: Snapshot): AssistantAnswer {
        val groups = snapshot.items
            .groupBy { it.storageLocation.trim().ifEmpty { UNKNOWN_LOCATION } }
            .map { (location, list) ->
                LocationGroup(
                    name = location,
                    count = list.size,
                    value = list.sumOf { it.purchasePrice }
                )
            }
            .sortedWith(compareByDescending<LocationGroup> { it.count }.thenByDescending { it.value })
        val filled = groups.filter { it.name != UNKNOWN_LOCATION }
        if (filled.isEmpty()) {
            return AssistantAnswer(
                text = "所有物品都还没填写存放位置，所以我还不知道东西都收在哪。",
                blocks = listOf(
                    AnswerBlock(
                        kind = AnswerBlock.Kind.TIP,
                        title = "填了位置之后",
                        lines = listOf(
                            "① 问「耳机在哪」我能直接报位置；",
                            "② 统计页会出现「存放位置分布」这一块；",
                            "③ 找东西时不用再翻箱倒柜。"
                        )
                    )
                ),
                suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？", "有哪些闲置了？")
            )
        }
        val top = groups.first()
        val lines = buildList {
            groups.take(LIST_LIMIT).forEach { group ->
                add("${group.name}：${count(group.count)} 件 · ${money(group.value)}")
            }
            if (groups.size > LIST_LIMIT) add("…另有 ${count(groups.size - LIST_LIMIT)} 处未列出")
        }
        return AssistantAnswer(
            text = "一共 ${count(groups.size)} 个存放位置，东西最多的是「${top.name}」：${count(top.count)} 件。",
            blocks = listOf(AnswerBlock(AnswerBlock.Kind.TEXT, "存放位置分布（按件数）", lines)),
            suggestions = listOf("我的耳机在哪？", "我的东西一共值多少钱？", "有哪些闲置了？")
        )
    }

    /** 模糊搜索：名称 / 备注 / 标签 / 存放位置。 */
    private fun searchAnswer(term: String, snapshot: Snapshot): AssistantAnswer {
        val needle = term.lowercase(Locale.getDefault())
        val matched = snapshot.items
            .filter { item ->
                item.name.lowercase(Locale.getDefault()).contains(needle) ||
                    item.description.lowercase(Locale.getDefault()).contains(needle) ||
                    item.tags.lowercase(Locale.getDefault()).contains(needle) ||
                    item.storageLocation.lowercase(Locale.getDefault()).contains(needle)
            }
            // 名称命中的排前面：搜「耳机」时「蓝牙耳机」应该比备注里提到耳机的东西更相关
            .sortedWith(
                compareByDescending<ItemEntity> { it.name.lowercase(Locale.getDefault()).contains(needle) }
                    .thenByDescending { it.purchasePrice }
            )
        if (matched.isEmpty()) {
            return AssistantAnswer(
                text = "没找到和「$term」相关的物品。可以换个更短的关键词，或者用物品名称里的字再问一次。",
                blocks = listOf(
                    AnswerBlock(
                        kind = AnswerBlock.Kind.TEXT,
                        title = "没有匹配结果",
                        lines = listOf("搜索会匹配：名称、备注、标签、存放位置。")
                    )
                ),
                suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？", "有哪些保修快到期了？")
            )
        }
        val first = matched.first()
        val text = if (matched.size == 1) {
            val location = first.storageLocation.trim().ifEmpty { "还没填写存放位置" }
            "「${first.name}」的位置是：$location。${statusHint(first)}"
        } else {
            "找到 ${count(matched.size)} 件与「$term」相关的物品，点开可以看详情。"
        }
        return AssistantAnswer(
            text = text,
            blocks = listOf(
                AnswerBlock(
                    kind = AnswerBlock.Kind.LIST,
                    title = "匹配「$term」的物品（${count(matched.size)} 件）",
                    lines = emptyList(),
                    items = matched.take(LIST_LIMIT)
                )
            ),
            suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？", "有哪些保修快到期了？")
        )
    }

    /** 省钱建议：保修窗口 / 长期闲置 / 资料完整度，三条规则。 */
    private fun adviceAnswer(snapshot: Snapshot): AssistantAnswer {
        val items = snapshot.items
        val days = snapshot.warrantyReminderDays.coerceIn(1, 365)
        val threshold = snapshot.now + days * DAY_MS
        val expiring = items.filter { item ->
            val expiry = item.warrantyExpiryDate ?: return@filter false
            ItemStatus.fromCode(item.status) != ItemStatus.SOLD && expiry <= threshold
        }.sortedBy { it.warrantyExpiryDate ?: Long.MAX_VALUE }
        val idle = items.filter { ItemStatus.fromCode(it.status) == ItemStatus.IDLE }
        val noPhoto = items.filter { it.id !in snapshot.photoItemIds }
        val noDescription = items.filter { it.description.isBlank() }

        val lines = mutableListOf<String>()
        lines += if (expiring.isNotEmpty()) {
            "① 保修：${count(expiring.size)} 件在 $days 天窗口内到期（${expiring.take(2).joinToString("、") { it.name }}），到期前验一次机、留好发票省得维修扯皮。"
        } else {
            "① 保修：近 $days 天没有到期，保持记录习惯就好。"
        }
        lines += if (idle.isNotEmpty()) {
            "② 闲置：${count(idle.size)} 件闲置（${idle.take(2).joinToString("、") { it.name }}），合计 ${money(idle.sumOf { it.purchasePrice })}；长期不用可以出掉或送人。"
        } else {
            "② 闲置：目前没有标记为闲置的物品。"
        }
        lines += when {
            noPhoto.isNotEmpty() -> "③ 资料：${count(noPhoto.size)} 件还没有照片、${count(noDescription.size)} 件没写备注；补一张照片下次更好认。"
            noDescription.isNotEmpty() -> "③ 资料：${count(noDescription.size)} 件还没写备注，补一句用途或购买渠道，日后更好查。"
            else -> "③ 资料：照片和备注都挺全的，保持住。"
        }
        return AssistantAnswer(
            text = "按保修、闲置和资料完整度，给你 3 条省钱又省心的建议：",
            blocks = listOf(AnswerBlock(AnswerBlock.Kind.TIP, "3 条建议", lines)),
            suggestions = listOf("有哪些保修快到期了？", "有哪些闲置了？", "我的东西一共值多少钱？")
        )
    }

    // ══════════════════════════════════════════════════════════
    //  小工具
    // ══════════════════════════════════════════════════════════

    /** 千分位整数（件数）。 */
    private fun count(value: Int): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).format(value.toLong())

    /** 金额：`¥` + 千分位，与 App 其它页面的写法一致。 */
    private fun money(value: Double): String =
        "¥" + NumberFormat.getNumberInstance(Locale.getDefault()).format(value)

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private fun statusCounts(items: List<ItemEntity>): Map<ItemStatus, Int> =
        ItemStatus.entries.associateWith { status -> items.count { ItemStatus.fromCode(it.status) == status } }

    /** 在持（在用+闲置+待修）金额，与首页「当前拥有」同口径。 */
    private fun ownedValue(items: List<ItemEntity>): Double =
        items.filter { ItemStatus.fromCode(it.status).isOwned }.sumOf { it.purchasePrice }

    private fun categoryName(snapshot: Snapshot, categoryId: Long): String =
        snapshot.categories.firstOrNull { it.id == categoryId }?.name ?: UNKNOWN_CATEGORY

    /** 非「在用」时补一句状态说明，避免用户点进详情才发现状态不对。 */
    private fun statusHint(item: ItemEntity): String {
        val status = ItemStatus.fromCode(item.status)
        return if (status == ItemStatus.ACTIVE) "" else "当前状态是「${status.label}」。"
    }

    private data class CategoryGroup(val name: String, val count: Int, val value: Double)

    private data class LocationGroup(val name: String, val count: Int, val value: Double)
}
