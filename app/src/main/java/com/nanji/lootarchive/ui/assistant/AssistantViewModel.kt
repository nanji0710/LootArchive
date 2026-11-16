package com.nanji.lootarchive.ui.assistant

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.assistant.AnswerBlock
import com.nanji.lootarchive.data.assistant.AssistantAnswer
import com.nanji.lootarchive.data.assistant.AssistantEngine
import com.nanji.lootarchive.data.assistant.ChatHistoryCodec
import com.nanji.lootarchive.data.assistant.RemoteAssistant
import com.nanji.lootarchive.data.local.entity.AssistantMessageEntity
import com.nanji.lootarchive.data.local.entity.ConversationEntity
import com.nanji.lootarchive.data.repository.AssistantHistoryRepository
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

    /** 消息 id 自增，只用于 LazyColumn 的 key 与「原地替换占位气泡」。 */
    /**
     * 运行时消息 id 用负数，数据库消息 id 是正数。
     * 两者混在同一个 LazyColumn 里时不会撞 key（之前都用 1、2…，历史恢复后再发消息会因重复 key 闪退）。
     */
    private var nextMessageId = 0L

    init {
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

            // 先看是不是「一句话新增物品」：是就直接落库并返回结果，不再走问答/远程模型。
            val draft = com.nanji.lootarchive.data.assistant.ItemDraftParser.parse(question)
            if (draft != null) {
                val category = latest.categories.firstOrNull { c ->
                    val hint = draft.categoryHint
                    hint != null && (c.name == hint || c.name.contains(hint) || hint.contains(c.name))
                } ?: latest.categories.firstOrNull()
                val now = System.currentTimeMillis()
                val item = com.nanji.lootarchive.data.local.entity.ItemEntity(
                    name = draft.name,
                    categoryId = category?.id ?: 0L,
                    purchasePrice = draft.price ?: 0.0,
                    purchaseDate = draft.purchaseDate ?: now,
                    warrantyExpiryDate = draft.expiryDate,
                    warrantyPeriodDays = draft.shelfLifeDays,
                    storageLocation = draft.location.orEmpty(),
                    description = listOfNotNull("由 AI 助理添加", draft.note).joinToString("；"),
                    status = "active",
                    updatedAt = now,
                )
                val savedId = runCatching { itemRepository.saveItemWithPhotos(item, emptyList()) }.getOrNull()
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
            val config = remoteConfig.value
            var remoteFailed = false
            val answer: AssistantAnswer = if (!config.isReady) {
                Log.d(logTag, "未配置远程模型，使用本地回答")
                localAnswer
            } else {
                val digest = engine.buildInventoryDigest(latest)
                remoteAssistant.ask(config, question, digest).fold(
                    onSuccess = { reply ->
                        Log.d(logTag, "模型回答成功：content=${reply.content.length}字, reasoning=${reply.reasoning.length}字")
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
