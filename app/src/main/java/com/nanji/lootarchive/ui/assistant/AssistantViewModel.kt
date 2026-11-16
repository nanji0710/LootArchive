package com.nanji.lootarchive.ui.assistant

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.assistant.AnswerBlock
import com.nanji.lootarchive.data.assistant.AssistantAnswer
import com.nanji.lootarchive.data.assistant.AssistantEngine
import com.nanji.lootarchive.data.assistant.ChatHistoryCodec
import com.nanji.lootarchive.data.assistant.ItemCategoryGuesser
import com.nanji.lootarchive.data.assistant.RemoteAssistant
import com.nanji.lootarchive.data.local.entity.AssistantMessageEntity
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ConversationEntity
import com.nanji.lootarchive.data.repository.AssistantHistoryRepository
import com.nanji.lootarchive.data.repository.AssistantMemoryRepository
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.domain.model.ItemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话里的一条消息。
 *
 * [pending]：用户发出问题后先插一个占位气泡（界面显示「思考中…」），回答落地时原地替换；
 * [error]：这条回答是「模型请求失败、已回退本地分析」产出的，界面据此给重试入口。
 */
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val blocks: List<AnswerBlock> = emptyList(),
    val pending: Boolean = false,
    val error: Boolean = false,
    /** 回答末尾的「接着问」建议；用户消息恒为空。 */
    val suggestions: List<String> = emptyList(),
    /** 模型返回的思考过程（reasoning_content），本地回答为空。 */
    val reasoning: String = "",
    /** 这条回答是否来自远程模型（用于界面标注与排查「到底走没走 API」）。 */
    val fromModel: Boolean = false,
)

/** 助理页的界面状态。 */
data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val thinking: Boolean = false,
    /** 是否配置好了远程模型（只有配置好才可能发网络请求，见 RemoteAssistant）。 */
    val remoteEnabled: Boolean = false,
    val itemCount: Int = 0,
    /** 在持物品合计金额（与首页「当前拥有」同口径）。 */
    val totalValue: Double = 0.0,
    val lastAnswer: AssistantAnswer? = null,
    /** 历史会话（按更新时间倒序），用于历史弹窗。 */
    val conversations: List<ConversationEntity> = emptyList(),
    /** 当前正在聊的会话 id；null 表示新会话，尚未落库。 */
    val currentConversationId: Long? = null,
)

/**
 * 助理页 ViewModel —— 负责三件事：聚合数据快照、把问题交给引擎/模型、维护会话状态。
 *
 * 分工刻意做成「ViewModel 管数据、AssistantEngine 管规则」：
 * 引擎是纯函数（无协程、无数据库、无网络），意图和文案的调整不需要碰 ViewModel 生命周期。
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val app: Application,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: AssistantHistoryRepository,
    private val memoryRepository: AssistantMemoryRepository,
) : ViewModel() {

    private val engine = AssistantEngine()
    private val remoteAssistant = RemoteAssistant()

    /** 日志前缀带包名：排查线上问题时能一眼看出是哪个进程/包。 */
    private val logTag = "${app.packageName}.AssistantVM"

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    /**
     * 数据快照（热流）：回答时直接读 `snapshot.value`，保证「问的那一刻」用的是最新数据。
     *
     * 用 Eagerly 而不是 WhileSubscribed：用户可能在首帧还没订阅状态时就用快捷提问发问，
     * 那时候也要能拿到真实数据，而不是空快照。
     */
    private val snapshot: StateFlow<AssistantEngine.Snapshot> = combine(
        itemRepository.getAllItems(),
        categoryRepository.getAllCategories(),
        settingsRepository.warrantyReminderDays
    ) { items, categories, days ->
        AssistantEngine.Snapshot(items = items, categories = categories, warrantyReminderDays = days)
    }
        .catch { e ->
            // 读库失败不应该让助理页整块崩掉：退化成空快照，回答会走「空库」文案
            Log.e(logTag, "读取物品/分类失败", e)
            emit(AssistantEngine.Snapshot())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AssistantEngine.Snapshot())

    /**
     * 远程模型配置。
     *
     * `assistantEnabled` 关掉时直接给一个空 Config → [RemoteAssistant.Config.isReady] 为 false
     * → 一个网络请求都不会发。这样「关掉开关」就等于「纯本地」，不需要在别处再加判断。
     */
    private val remoteConfig: StateFlow<RemoteAssistant.Config> = combine(
        settingsRepository.assistantEnabled,
        settingsRepository.assistantEndpoint,
        settingsRepository.assistantApiKey,
        settingsRepository.assistantModel,
        settingsRepository.assistantReasoningEffort,
    ) { enabled, endpoint, apiKey, model, effort ->
        if (enabled) RemoteAssistant.Config(endpoint, apiKey, model, effort) else RemoteAssistant.Config()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RemoteAssistant.Config())

    private var sendJob: Job? = null

    /**
     * 当前会话的操作上下文（对应文档里的 OperateContext）。
     * 只存结构化字段，不把整段聊天当记忆：指代「它/这个」直接看 lastItemId，参数补全看 lastCategory/lastLocation。
     */
    private data class OperateContext(
        var lastItemId: Long? = null,
        var lastItemName: String? = null,
        var lastCategory: String = "",
        var lastLocation: String = "",
        var lastOperationAt: Long = 0L,
    )

    private var operateContext = OperateContext()

    /** 消息 id 自增，只用于 LazyColumn 的 key 与「原地替换占位气泡」。 */
    /**
     * 运行时消息 id 用负数，数据库消息 id 是正数。
     * 两者混在同一个 LazyColumn 里时不会撞 key（之前都用 1、2…，历史恢复后再发消息会因重复 key 闪退）。
     */
    private var nextMessageId = 0L

    init {
        // 清掉很久没被用到的低重要度记忆，避免越存越多
        viewModelScope.launch { runCatching { memoryRepository.prune() } }
        // 顶栏副标题（件数 / 合计金额）跟着物品数据走；用 update 合并，避免覆盖聊天状态
        viewModelScope.launch {
            snapshot.collect { current ->
                val ownedValue = current.items
                    .filter { ItemStatus.fromCode(it.status).isOwned }
                    .sumOf { it.purchasePrice }
                _uiState.update { it.copy(itemCount = current.items.size, totalValue = ownedValue) }
            }
        }
        viewModelScope.launch {
            remoteConfig.collect { config ->
                _uiState.update { it.copy(remoteEnabled = config.isReady) }
            }
        }
        // 历史会话列表 + 上次会话恢复：沟通记录保存在本地 Room
        viewModelScope.launch {
            historyRepository.conversations().collect { list ->
                _uiState.update { it.copy(conversations = list) }
            }
        }
        viewModelScope.launch {
            runCatching {
                val latest = historyRepository.latestConversation() ?: return@runCatching
                if (_uiState.value.currentConversationId != null || _uiState.value.messages.isNotEmpty()) return@runCatching
                val history = historyRepository.listMessages(latest.id).map { it.toChatMessage() }
                if (history.isNotEmpty()) {
                    _uiState.update { it.copy(currentConversationId = latest.id, messages = history) }
                }
            }
        }
    }

    fun updateInput(text: String) {
        // 单条提问限长：既防误粘贴巨量文本，也避免把超长内容带进远程请求
        _uiState.update { it.copy(input = text.take(MAX_INPUT_LENGTH)) }
    }

    /**
     * 发送一条提问：先插入用户气泡 + 占位气泡，再算回答，最后原地替换。
     *
     * 回答策略：
     * 1. 没配置模型 → 纯本地回答（不发任何网络请求）；
     * 2. 配置了 → 先问模型；模型失败则用本地回答，并附上「模型请求失败，已用本地分析回答」提示块。
     */
    fun send(text: String) {
        val question = text.trim()
        // thinking 期间拒绝重复发送：既避免两个回答抢同一个占位气泡，也省一次模型请求
        if (question.isEmpty() || _uiState.value.thinking) return
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            val userId = nextId()
            val replyId = nextId()
            // 先记住发送前的上下文，远程模型才能理解「它」「刚刚那个」指的是什么
            val historyForModel = _uiState.value.messages
                .filter { !it.pending && it.text.isNotBlank() }
                .takeLast(8)
                .map { RemoteAssistant.ChatTurn(fromUser = it.fromUser, text = it.text) }
            _uiState.update { state ->
                state.copy(
                    input = "",
                    thinking = true,
                    messages = state.messages +
                        ChatMessage(id = userId, fromUser = true, text = question) +
                        ChatMessage(id = replyId, fromUser = false, text = "", pending = true)
                )
            }

            val latest = refreshSnapshot()
            val config = remoteConfig.value
            // 长期记忆 + 本地习惯：按当前问题检索记忆，再带上常用分类/位置，注入 System Prompt
            val memoryLines = buildList {
                memoryRepository.relevant(question).forEach { add("• ${it.content}") }
                buildHabitMemory(latest)?.let { add(it) }
            }
            val memoryText = memoryLines.joinToString("\n")

            // 历史持久化：确定当前会话（新会话首条提问作为标题），并先写入用户消息
            val convId = _uiState.value.currentConversationId
                ?: historyRepository.createConversation(question.take(24)).also { id ->
                    _uiState.update { it.copy(currentConversationId = id) }
                }
            runCatching {
                val title = _uiState.value.conversations.firstOrNull { it.id == convId }?.title
                    ?: question.take(24)
                historyRepository.touchConversation(convId, title)
                historyRepository.appendMessage(convId, true, question)
            }

            // 有 API 时先让模型做语义抽取：它能结合上下文判断是「新增」还是「修改上一条」，
            // 并按真实分类列表选分类，不靠本地关键词写死。
            val currentItem = latest.items.firstOrNull { it.id == operateContext.lastItemId }
            val currentItemSummary = currentItem?.let {
                val currentCategory = latest.categories.firstOrNull { c -> c.id == it.categoryId }?.name ?: "未分类"
                "名称=${it.name}，价格=${it.purchasePrice}，购买日期=${it.purchaseDate?.let { d -> com.nanji.lootarchive.util.DateUtil.formatTimestamp(d) } ?: "未填"}，保修天数=${it.warrantyPeriodDays ?: "未填"}，分类=$currentCategory，位置=${it.storageLocation.ifBlank { "未填" }}"
            }
            val itemAction = if (config.isReady && looksLikeItemAction(question)) {
                remoteAssistant.extractItemAction(
                    config = config,
                    question = question,
                    categories = latest.categories.map { it.name },
                    lastItemName = operateContext.lastItemName,
                    currentItemSummary = currentItemSummary,
                    history = historyForModel,
                ).getOrNull()
            } else {
                null
            }
            // 模型偶尔会把「纠正上一条」误判成 add；这种错误会新建一个垃圾物品，必须拦住
            val correctionCue = containsDateExpression(question) || listOf(
                "改", "不是", "错", "名称", "名字", "分类", "价格"
            ).any { question.contains(it) }
            val safeItemAction = if (
                itemAction != null && itemAction.action == "add" &&
                operateContext.lastItemId != null && correctionCue
            ) {
                Log.w(logTag, "模型把修改请求误判为新增，已忽略，改走字段校正")
                null
            } else {
                itemAction
            }
            if (safeItemAction != null && safeItemAction.action == "add") {
                val itemAction = safeItemAction
                val now = System.currentTimeMillis()
                val purchaseDate = parseDate(itemAction.purchaseDate)
                    ?: parseDateFromText(itemAction.purchaseDate, null)
                    ?: now
                // 用户没给分类/位置时，沿用上一次操作的值（储物场景的常用值补全）
                val categoryHint = itemAction.category.takeIf { it.isNotBlank() }
                    ?: operateContext.lastCategory.takeIf { it.isNotBlank() }
                val location = itemAction.location.ifBlank { operateContext.lastLocation }
                val category = resolveCategory(categoryHint, itemAction.itemName, latest.categories)
                val expiry = itemAction.shelfLifeDays?.let { purchaseDate + it * 24L * 60 * 60 * 1000 }
                val item = com.nanji.lootarchive.data.local.entity.ItemEntity(
                    name = itemAction.itemName.ifBlank { "未命名物品" },
                    categoryId = category?.id ?: 0L,
                    purchasePrice = itemAction.price ?: 0.0,
                    purchaseDate = purchaseDate,
                    warrantyExpiryDate = expiry,
                    warrantyPeriodDays = itemAction.shelfLifeDays,
                    storageLocation = location,
                    description = itemAction.note,
                    status = "active",
                    updatedAt = now,
                )
                val savedId = runCatching { itemRepository.saveItemWithPhotos(item, emptyList()) }.getOrNull()
                if (savedId != null) {
                    operateContext.lastItemId = savedId
                    operateContext.lastItemName = item.name
                    operateContext.lastCategory = category?.name.orEmpty()
                    operateContext.lastLocation = location
                    operateContext.lastOperationAt = now
                }
                val reply = AssistantAnswer(
                    text = if (savedId != null) "已新增「${item.name}」，分类是「${category?.name ?: "其他"}」。"
                    else "新增「${item.name}」失败，请稍后再试。",
                    blocks = listOf(
                        AnswerBlock(
                            kind = AnswerBlock.Kind.SUMMARY,
                            title = if (savedId != null) "已新增物品" else "保存失败",
                            lines = buildList {
                                add("名称|${item.name}")
                                itemAction.price?.let { add("价格|¥${"%.2f".format(it)}") }
                                add("购买日期|${com.nanji.lootarchive.util.DateUtil.formatTimestamp(purchaseDate)}")
                                itemAction.shelfLifeDays?.let { add("保质期|$it 天") }
                                expiry?.let { add("到期日|${com.nanji.lootarchive.util.DateUtil.formatTimestamp(it)}") }
                                category?.let { add("分类|${it.name}") }
                                if (itemAction.location.isNotBlank()) add("位置|${itemAction.location}")
                            },
                        )
                    ),
                    suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？"),
                    fromModel = true,
                )
                _uiState.update { state ->
                    state.copy(
                        thinking = false,
                        lastAnswer = reply,
                        messages = state.messages.map { message ->
                            if (message.id != replyId) message
                            else ChatMessage(replyId, false, reply.text, reply.blocks, suggestions = reply.suggestions)
                        }
                    )
                }
                runCatching { historyRepository.appendMessage(convId, false, reply.text, reply.blocks, reply.suggestions) }
                return@launch
            }
            if (safeItemAction != null && safeItemAction.action == "update" && operateContext.lastItemId != null) {
                val itemAction = safeItemAction
                val existing = runCatching { itemRepository.getItemById(operateContext.lastItemId!!) }.getOrNull()
                if (existing != null) {
                    // 只改日期的句子绝不能碰名称：例如「是今年11月，不是25年11月」
                    val dateOnly = containsDateExpression(question) &&
                        !question.contains("名字") && !question.contains("名称")
                    val newName = if (dateOnly) existing.name else itemAction.itemName.ifBlank { existing.name }
                    val newCategory = if (itemAction.category.isNotBlank() && !dateOnly) {
                        resolveCategory(itemAction.category, newName, latest.categories)
                    } else null
                    val newPurchase = parseDate(itemAction.purchaseDate)
                        ?: if (dateOnly) parseDateFromText(question, existing.purchaseDate) else null
                    val newPeriod = itemAction.shelfLifeDays ?: existing.warrantyPeriodDays
                    val newExpiry = if (itemAction.shelfLifeDays != null) {
                        (newPurchase ?: existing.purchaseDate ?: System.currentTimeMillis()) +
                            itemAction.shelfLifeDays * 24L * 60 * 60 * 1000
                    } else existing.warrantyExpiryDate
                    val updated = existing.copy(
                        name = newName,
                        categoryId = newCategory?.id ?: existing.categoryId,
                        purchasePrice = itemAction.price ?: existing.purchasePrice,
                        purchaseDate = newPurchase ?: existing.purchaseDate,
                        storageLocation = itemAction.location.ifBlank { existing.storageLocation },
                        warrantyPeriodDays = newPeriod,
                        warrantyExpiryDate = newExpiry,
                        description = itemAction.note.ifBlank { existing.description },
                        updatedAt = System.currentTimeMillis(),
                    )
                    val ok = runCatching { itemRepository.updateItem(updated) }.isSuccess
                    operateContext.lastItemName = newName
                    newCategory?.let { operateContext.lastCategory = it.name }
                    if (itemAction.location.isNotBlank()) operateContext.lastLocation = itemAction.location
                    operateContext.lastOperationAt = System.currentTimeMillis()
                    val reply = AssistantAnswer(
                        text = if (ok) "已把「${existing.name}」改成「$newName」，其他信息保持不变。"
                        else "修改失败，请稍后再试。",
                        blocks = if (ok) listOf(
                            AnswerBlock(
                                kind = AnswerBlock.Kind.SUMMARY,
                                title = "已修改物品",
                                lines = buildList {
                                    add("名称|$newName")
                                    newCategory?.let { add("分类|${it.name}") }
                                    itemAction.price?.let { add("价格|¥${"%.2f".format(it)}") }
                                    itemAction.shelfLifeDays?.let { add("保质期|$it 天") }
                                    if (itemAction.location.isNotBlank()) add("位置|${itemAction.location}")
                                },
                            )
                        ) else emptyList(),
                        suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？"),
                        fromModel = true,
                    )
                    _uiState.update { state ->
                        state.copy(
                            thinking = false,
                            lastAnswer = reply,
                            messages = state.messages.map { message ->
                                if (message.id != replyId) message
                                else ChatMessage(replyId, false, reply.text, reply.blocks, suggestions = reply.suggestions)
                            }
                        )
                    }
                    runCatching { historyRepository.appendMessage(convId, false, reply.text, reply.blocks, reply.suggestions) }
                    return@launch
                }
            }

            // 本地字段校正：只有没配置 API 或模型没给出可执行动作时才启用。
            // 这样不会出现「显示已接入模型、实际偷偷走本地规则」的情况；模型动作有效时永远优先执行模型结果。
            val lastId = operateContext.lastItemId
            if (lastId != null && (!config.isReady || safeItemAction == null)) {
                val edits = com.nanji.lootarchive.data.assistant.ItemDraftParser.parseEdits(question)
                val hasDate = containsDateExpression(question)
                if (edits != null || hasDate) {
                    val existing = runCatching { itemRepository.getItemById(lastId) }.getOrNull()
                    if (existing != null) {
                        val now = System.currentTimeMillis()
                        val newName = edits?.name?.takeIf { it.isNotBlank() } ?: existing.name
                        val newCategoryId = edits?.categoryHint?.let { resolveCategoryId(it, newName, latest.categories) }
                            ?: existing.categoryId
                        val newPurchase = edits?.purchaseDate
                            ?: if (hasDate) parseDateFromText(question, existing.purchaseDate) else null
                        val newPeriod = edits?.shelfLifeDays ?: existing.warrantyPeriodDays
                        val newExpiry = if (edits?.shelfLifeDays != null) {
                            (newPurchase ?: existing.purchaseDate ?: now) + edits.shelfLifeDays * 24L * 60 * 60 * 1000
                        } else existing.warrantyExpiryDate
                        val updated = existing.copy(
                            name = newName,
                            categoryId = newCategoryId,
                            purchasePrice = edits?.price ?: existing.purchasePrice,
                            purchaseDate = newPurchase ?: existing.purchaseDate,
                            storageLocation = edits?.location ?: existing.storageLocation,
                            warrantyPeriodDays = newPeriod,
                            warrantyExpiryDate = newExpiry,
                            description = edits?.note ?: existing.description,
                            updatedAt = now,
                        )
                        val ok = runCatching { itemRepository.updateItem(updated) }.isSuccess
                        operateContext.lastItemName = newName
                        val categoryName = latest.categories.firstOrNull { it.id == newCategoryId }?.name
                        val changed = buildList {
                            if (newName != existing.name) add("名称改为「$newName」")
                            if (newPurchase != null && newPurchase != existing.purchaseDate) {
                                add("购买日期改为 ${com.nanji.lootarchive.util.DateUtil.formatTimestamp(newPurchase)}")
                            }
                            if (edits?.price != null) add("价格改为 ¥${"%.2f".format(edits.price)}")
                            if (edits?.shelfLifeDays != null) add("保质期改为 ${edits.shelfLifeDays} 天")
                            if (!edits?.location.isNullOrBlank()) add("位置改为 ${edits.location}")
                            if (edits?.categoryHint != null) add("分类改为「${categoryName ?: edits.categoryHint}」")
                        }.ifEmpty { listOf("信息未变化") }
                        val reply = AssistantAnswer(
                            text = if (ok) "已修改「${existing.name}」：${changed.joinToString("，")}。" else "修改失败了，请稍后再试。",
                            blocks = if (ok) listOf(
                                AnswerBlock(
                                    kind = AnswerBlock.Kind.SUMMARY,
                                    title = "已修改物品",
                                    lines = buildList {
                                        add("名称|$newName")
                                        categoryName?.let { add("分类|$it") }
                                        newPurchase?.let { add("购买日期|${com.nanji.lootarchive.util.DateUtil.formatTimestamp(it)}") }
                                        edits?.price?.let { add("价格|¥${"%.2f".format(it)}") }
                                        edits?.shelfLifeDays?.let { add("保质期|$it 天") }
                                        if (!edits?.location.isNullOrBlank()) add("位置|${edits.location}")
                                    },
                                )
                            ) else emptyList(),
                            suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？"),
                        )
                        _uiState.update { state ->
                            state.copy(
                                thinking = false,
                                lastAnswer = reply,
                                messages = state.messages.map { message ->
                                    if (message.id != replyId) message
                                    else ChatMessage(
                                        id = replyId,
                                        fromUser = false,
                                        text = reply.text,
                                        blocks = reply.blocks,
                                        suggestions = reply.suggestions,
                                    )
                                }
                            )
                        }
                        runCatching {
                            historyRepository.appendMessage(convId, false, reply.text, reply.blocks, reply.suggestions)
                        }
                        return@launch
                    }
                }
            }
            // 先看是不是「一句话新增物品」：是就直接落库并返回结果，不再走问答/远程模型。
            val draft = com.nanji.lootarchive.data.assistant.ItemDraftParser.parse(question)
            if (draft != null && !config.isReady) {
                val categoryHint = draft.categoryHint ?: operateContext.lastCategory.takeIf { it.isNotBlank() }
                val category = resolveCategory(categoryHint, draft.name, latest.categories)
                val location = draft.location.orEmpty().ifBlank { operateContext.lastLocation }
                val now = System.currentTimeMillis()
                val item = com.nanji.lootarchive.data.local.entity.ItemEntity(
                    name = draft.name,
                    categoryId = category?.id ?: 0L,
                    purchasePrice = draft.price ?: 0.0,
                    purchaseDate = draft.purchaseDate ?: now,
                    warrantyExpiryDate = draft.expiryDate,
                    warrantyPeriodDays = draft.shelfLifeDays,
                    storageLocation = location,
                    description = listOfNotNull("由 AI 助理添加", draft.note).joinToString("；"),
                    status = "active",
                    updatedAt = now,
                )
                val savedId = runCatching { itemRepository.saveItemWithPhotos(item, emptyList()) }.getOrNull()
                if (savedId != null) {
                    operateContext.lastItemId = savedId
                    operateContext.lastItemName = draft.name
                    operateContext.lastCategory = category?.name.orEmpty()
                    operateContext.lastLocation = location
                    operateContext.lastOperationAt = now
                }
                val lines = buildList {
                    add("名称|${draft.name}")
                    draft.price?.let { add("价格|¥${"%.2f".format(it)}") }
                    add("购买日期|${com.nanji.lootarchive.util.DateUtil.formatTimestamp(draft.purchaseDate ?: now)}")
                    draft.shelfLifeDays?.let { add("保质期|$it 天") }
                    draft.expiryDate?.let { add("到期日|${com.nanji.lootarchive.util.DateUtil.formatTimestamp(it)}") }
                    category?.let { add("分类|${it.name}") }
                    if (!draft.location.isNullOrBlank()) add("位置|${draft.location}")
                }
                val reply = if (savedId != null) {
                    AssistantAnswer(
                        text = "已自动新增「${draft.name}」，可以在首页或物品列表里查看。",
                        blocks = listOf(
                            AnswerBlock(
                                kind = AnswerBlock.Kind.SUMMARY,
                                title = "已新增物品",
                                lines = lines,
                            )
                        ),
                        suggestions = listOf("我的东西一共值多少钱？", "最近添加了什么？"),
                    )
                } else {
                    AssistantAnswer(
                        text = "自动新增「${draft.name}」失败，请稍后再试，或到「新增物品」页手动填写。",
                        blocks = listOf(AnswerBlock(AnswerBlock.Kind.TIP, "保存失败", listOf("数据库写入失败，未添加任何物品。")))
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        thinking = false,
                        lastAnswer = reply,
                        messages = state.messages.map { message ->
                            if (message.id != replyId) message
                            else ChatMessage(
                                id = replyId,
                                fromUser = false,
                                text = reply.text,
                                blocks = reply.blocks,
                                suggestions = reply.suggestions,
                            )
                        }
                    )
                }
                runCatching {
                    historyRepository.appendMessage(
                        convId, false, reply.text, reply.blocks, reply.suggestions
                    )
                }
                return@launch
            }

            val localAnswer = engine.answer(question, latest)
            var remoteFailed = false
            val answer: AssistantAnswer = if (!config.isReady) {
                Log.d(logTag, "未配置远程模型，使用本地回答")
                localAnswer
            } else {
                val digest = engine.buildInventoryDigest(latest)
                remoteAssistant.ask(config, question, digest, historyForModel, memoryText).fold(
                    onSuccess = { reply ->
                        Log.d(logTag, "模型回答成功：content=${reply.content.length}字, reasoning=${reply.reasoning.length}字")
                        if (shouldRemember(question)) {
                            viewModelScope.launch {
                                val turns = historyForModel +
                                    RemoteAssistant.ChatTurn(true, question) +
                                    RemoteAssistant.ChatTurn(false, reply.content)
                                remoteAssistant.extractMemories(config, turns)
                                    .onSuccess { memories ->
                                        memories.forEach { m ->
                                            memoryRepository.remember(m.type, m.content, m.keywords, m.importance)
                                        }
                                    }
                            }
                        }
                        // 模型话术为主，本地结构化块保留（物品条目仍可点进详情）；
                        // 兜底说明块丢掉 —— 模型已经正常回答了，不需要再解释「我能做什么」。
                        AssistantAnswer(
                            text = reply.content,
                            blocks = localAnswer.blocks.filter { it.kind != AnswerBlock.Kind.UNKNOWN },
                            suggestions = localAnswer.suggestions,
                            reasoning = reply.reasoning,
                            fromModel = true,
                        )
                    },
                    onFailure = { error ->
                        remoteFailed = true
                        Log.w(logTag, "模型请求失败，回退本地分析", error)
                        localAnswer.copy(blocks = localAnswer.blocks + fallbackNoticeBlock(error))
                    }
                )
            }

            _uiState.update { state ->
                state.copy(
                    thinking = false,
                    lastAnswer = answer,
                    messages = state.messages.map { message ->
                        if (message.id != replyId) {
                            message
                        } else {
                            ChatMessage(
                                id = replyId,
                                fromUser = false,
                                text = answer.text,
                                blocks = answer.blocks,
                                error = remoteFailed,
                                suggestions = answer.suggestions,
                                reasoning = answer.reasoning,
                                fromModel = answer.fromModel,
                            )
                        }
                    }
                )
            }
            runCatching {
                historyRepository.appendMessage(
                    convId, false, answer.text, answer.blocks, answer.suggestions, remoteFailed
                )
            }
        }
    }

    /** 清空/开启新会话：旧会话仍保留在「历史沟通」里，可随时找回。 */
    fun clearChat() {
        startNewConversation()
    }

    /** 新建会话：不删除任何历史，只是从空白开始。 */
    fun startNewConversation() {
        sendJob?.cancel()
        sendJob = null
        _uiState.update {
            it.copy(messages = emptyList(), input = "", thinking = false, lastAnswer = null, currentConversationId = null)
        }
    }

    /** 打开一条历史会话。 */
    fun openConversation(conversationId: Long) {
        if (_uiState.value.thinking) return
        sendJob?.cancel()
        sendJob = null
        viewModelScope.launch {
            val history = runCatching { historyRepository.listMessages(conversationId) }
                .getOrDefault(emptyList())
                .map { it.toChatMessage() }
            _uiState.update {
                it.copy(
                    currentConversationId = conversationId,
                    messages = history,
                    thinking = false,
                    lastAnswer = null,
                )
            }
        }
    }

    /** 批量删除历史会话（可多选）。 */
    fun deleteConversations(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            historyRepository.deleteConversations(ids.toList())
            if (_uiState.value.currentConversationId in ids) {
                startNewConversation()
            }
        }
    }

    /** 重试上一条提问（模型失败后最常见的动作）：把最后一条用户消息之后的内容全部作废再重发。 */
    fun retryLast() {
        if (_uiState.value.thinking) return
        val messages = _uiState.value.messages
        val lastUserIndex = messages.indexOfLast { it.fromUser }
        if (lastUserIndex < 0) return
        val question = messages[lastUserIndex].text
        _uiState.update { state ->
            state.copy(messages = state.messages.take(lastUserIndex), thinking = false)
        }
        send(question)
    }

    /**
     * 回答前取一次最新快照。
     *
     * [snapshot] 是热流，通常已经是最新值；这里额外补一次「有照片的物品 id」，
     * 因为「缺照片」建议要判断哪些物品还没有照片，而这来自 ItemPhotoDao（不在 Snapshot 流里）。
     * 取失败就当作没有照片信息，建议照常给，不阻塞回答。
     */
    private suspend fun refreshSnapshot(): AssistantEngine.Snapshot {
        val base = snapshot.value
        val photoIds = runCatching { itemRepository.getAllFirstPhotos().keys }
            .getOrElse { emptySet() }
        return base.copy(photoItemIds = photoIds, now = System.currentTimeMillis())
    }

    /** 分类解析：明确写了分类就用分类名；否则按物品名猜，最后回退到「其他」。 */
    private fun resolveCategory(
        hint: String?,
        name: String,
        categories: List<CategoryEntity>,
    ): CategoryEntity? {
        val wanted = hint?.trim()?.takeIf { it.isNotBlank() } ?: ItemCategoryGuesser.guess(name)
        val matched = categories.firstOrNull { c ->
            wanted != null && (c.name == wanted || c.name.contains(wanted) || wanted.contains(c.name))
        }
        return matched
            ?: categories.firstOrNull { it.name == "其他" }
            ?: categories.firstOrNull()
    }

    private fun resolveCategoryId(
        hint: String?,
        name: String,
        categories: List<CategoryEntity>,
    ): Long = resolveCategory(hint, name, categories)?.id ?: 0L

    /** 从本地物品统计常用分类/位置，作为跨会话的习惯记忆注入（只取前 3 个）。 */
    private fun buildHabitMemory(snapshot: AssistantEngine.Snapshot): String? {
        if (snapshot.items.isEmpty()) return null
        val categoryCounts = snapshot.items
            .mapNotNull { item -> snapshot.categories.firstOrNull { it.id == item.categoryId }?.name }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(3).map { it.key }
        val locationCounts = snapshot.items
            .map { it.storageLocation.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(3).map { it.key }
        if (categoryCounts.isEmpty() && locationCounts.isEmpty()) return null
        return buildString {
            if (categoryCounts.isNotEmpty()) append("用户常用分类：${categoryCounts.joinToString("、")}")
            if (categoryCounts.isNotEmpty() && locationCounts.isNotEmpty()) append('\n')
            if (locationCounts.isNotEmpty()) append("用户常用位置：${locationCounts.joinToString("、")}")
        }
    }

    /** 触发模型做「新增/修改物品」语义抽取的入口词。普通提问不会走这条，避免多一次请求。 */
    private fun looksLikeItemAction(text: String): Boolean = listOf(
        "新增", "添加", "加个", "加一个", "买了", "入手", "记一下", "记录物品",
        "改成", "改一下", "修改", "名字写错", "名称写错", "换个名字",
        "分类改", "类别改", "价格改", "保质期改", "日期", "月份", "今年", "去年", "明年",
        "不是", "名称", "名字", "年", "月", "日", "号",
    ).any { text.contains(it) }

    private fun containsDateExpression(text: String): Boolean =
        Regex("(\\d{2,4}\\s*[年./-]|\\d{1,2}\\s*月|今天|今年|去年|明年|昨天|前天)").containsMatchIn(text)

    /** 只有用户表达了偏好、固定事实或明确要求记住时才提取长期记忆，避免每轮多一次请求。 */
    private fun shouldRemember(text: String): Boolean = listOf(
        "记住", "以后", "我叫", "我是", "我喜欢", "我不喜欢", "我养", "我对",
        "我习惯", "平时", "每次", "过敏", "生日", "家里", "我的职业", "我住",
    ).any { text.contains(it) }

    private fun parseDate(text: String): Long? {
        if (text.isBlank()) return null
        return runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).parse(text)?.time
        }.getOrNull()
    }

    /** 解析「今年11月」「26年11月10号」这类不完整日期；日缺省时沿用原购买日。 */
    private fun parseDateFromText(rawText: String, base: Long?): Long? {
        val text = rawText
            .replace("十一月", "11").replace("十二月", "12")
            .replace("十月", "10").replace("九月", "9")
            .replace("八月", "8").replace("七月", "7")
            .replace("六月", "6").replace("五月", "5")
            .replace("四月", "4").replace("三月", "3")
            .replace("二月", "2").replace("一月", "1")
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = base ?: System.currentTimeMillis()
        }
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val baseDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val full = Regex("(\\d{4})\\s*[年./-]\\s*(\\d{1,2})(?:\\s*[月./-]\\s*(\\d{1,2})\\s*[日号]?)?").find(text)
        val shortYear = Regex("(\\d{2})\\s*年\\s*(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*[日号]?)?").find(text)
        val monthOnly = Regex("(\\d{1,2})\\s*月(?:\\s*(\\d{1,2})\\s*[日号]?)?").find(text)
        // 「今年11月，不是25年11月」以「今年」为准，不能被后面的旧年份抢走
        if (text.contains("今年") && full == null) {
            val m = monthOnly ?: return null
            val (month, day) = m.destructured
            cal.clear(); cal.set(currentYear, month.toInt() - 1, day.toIntOrNull() ?: baseDay, 12, 0, 0)
            return cal.timeInMillis
        }
        return when {
            full != null -> {
                val (y, m, d) = full.destructured
                cal.clear(); cal.set(y.toInt(), m.toInt() - 1, d.toIntOrNull() ?: baseDay, 12, 0, 0); cal.timeInMillis
            }
            shortYear != null -> {
                val (y, m, d) = shortYear.destructured
                cal.clear(); cal.set(2000 + y.toInt(), m.toInt() - 1, d.toIntOrNull() ?: baseDay, 12, 0, 0); cal.timeInMillis
            }
            monthOnly != null -> {
                val (m, d) = monthOnly.destructured
                val year = when {
                    text.contains("去年") -> currentYear - 1
                    text.contains("明年") -> currentYear + 1
                    else -> currentYear
                }
                cal.clear(); cal.set(year, m.toInt() - 1, d.toIntOrNull() ?: baseDay, 12, 0, 0); cal.timeInMillis
            }
            else -> null
        }
    }

    /** 模型失败时的提示块：明确告诉用户「不是没回答，是改用本地分析」。 */
    private fun fallbackNoticeBlock(error: Throwable): AnswerBlock {
        val reason = error.message?.take(60)?.ifBlank { "网络异常" } ?: "网络异常"
        return AnswerBlock(
            kind = AnswerBlock.Kind.TIP,
            title = "模型暂不可用",
            lines = listOf(
                "模型请求失败，已用本地分析回答。",
                "可以在设置里检查接口地址、模型名与密钥（$reason）。"
            )
        )
    }

    private fun nextId(): Long = --nextMessageId

    private companion object {
        /** 单条提问上限：够写一整句，也挡得住误粘贴。 */
        private const val MAX_INPUT_LENGTH = 300

/** 数据库消息 → 界面消息。 */
private fun AssistantMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    fromUser = fromUser,
    text = text,
    blocks = ChatHistoryCodec.decodeBlocks(blocksJson),
    error = error,
    suggestions = ChatHistoryCodec.decodeSuggestions(suggestionsJson),
)
    }
}
