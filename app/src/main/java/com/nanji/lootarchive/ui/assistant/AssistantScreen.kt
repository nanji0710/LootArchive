package com.nanji.lootarchive.ui.assistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.data.local.entity.ConversationEntity
import com.nanji.lootarchive.ui.component.PageTopBar
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidIconButton
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab 模式下底部悬浮导航条的占位高度：导航条 64dp + 16dp 外边距（见 LiquidGlassBottomBar）。
 *
 * 输入栏必须让出这段，否则会被悬浮底栏压住；切到二级页（isTabMode = false）时没有底栏，不需要让。
 */
private val TabBottomBarClearance = 64.dp + AppSpacing.xl

/**
 * 拾物助手页。
 *
 * 两种挂载方式共用同一套内容：
 * - [isTabMode] = true：作为底部导航的一级页，不画返回键，标题旁给设置入口；
 * - [isTabMode] = false：作为二级页，用 [PageTopBar]（玻璃返回按钮 + 标题）。
 *
 * @param onNavigateToDetail 回答里的物品条目点击后跳详情
 * @param onNavigateToStats 输入栏兜底入口：看统计
 * @param onNavigateToAddItem 输入栏兜底入口：新增物品
 * @param onNavigateToSettings 顶栏设置入口（模型接口/开关在设置页）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    onNavigateBack: () -> Unit = {},
    isTabMode: Boolean = false,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var inputFocusTick by remember { mutableIntStateOf(0) }

    // 输入栏底部只留一段：键盘高度和悬浮导航栏高度取较大值，不做加法。
    // 之前 Scaffold 的 ime padding 和固定导航栏留白叠加，键盘弹出时会多出一大片空白。
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val tabClearancePx = with(density) { TabBottomBarClearance.roundToPx() }
    val inputBottomPadding = with(density) {
        maxOf(imeBottomPx, if (isTabMode) tabClearancePx else navBottomPx).toDp()
    }

    val bgBrush = backgroundBrush()
    // 顶栏玻璃按钮的捕获层只画背景刷：这一层里没有按钮自己，所以不会自采样
    val topBarBackdrop = rememberLayerBackdrop { drawRect(brush = bgBrush) }

    // 新消息（含占位气泡）落地后滚到底：reverseLayout 下 index 0 就是最新一条
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    // 键盘弹出/收起时也保持贴底：否则列表高度变小后最后几条会被输入框和键盘盖住
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    // 输入框获得焦点后等键盘动画结束再贴底一次，覆盖输入法 inset 更新有延迟的机型
    LaunchedEffect(inputFocusTick) {
        if (inputFocusTick > 0) {
            kotlinx.coroutines.delay(350)
            if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isTabMode) {
                AssistantTabHeader(
                    subtitle = buildString {
                        append("本地 ${uiState.itemCount} 件物品 · 合计 ¥${numberFormat.format(uiState.totalValue)}")
                        append(if (uiState.remoteEnabled) " · 已接入模型" else " · 纯本地分析")
                    },
                    hasMessages = uiState.messages.isNotEmpty(),
                    backdrop = topBarBackdrop,
                    onClearChat = { showClearDialog = true },
                    onOpenHistory = { showHistoryDialog = true },
                )
            } else {
                PageTopBar(
                    title = "拾物助手",
                    onNavigateBack = onNavigateBack,
                    backdrop = topBarBackdrop,
                    trailing = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.messages.isNotEmpty()) {
                                ClearChatButton(topBarBackdrop) { showClearDialog = true }
                            }
                            HistoryButton(topBarBackdrop) { showHistoryDialog = true }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (uiState.messages.isEmpty()) {
                AssistantWelcome(onSend = viewModel::send, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    // 聊天列表倒序铺：最新消息固定在底部，键盘弹起/收起改变高度时不会把底部内容顶丢
                    reverseLayout = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.pageEdge,
                        end = AppSpacing.pageEdge,
                        top = AppSpacing.xl,
                        bottom = AppSpacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                ) {
                    // 数据仍按时间从旧到新保存，渲染时反过来，让 index 0 = 最新一条
                    items(items = uiState.messages.asReversed(), key = { it.id }) { message ->
                        AssistantMessageRow(
                            message = message,
                            numberFormat = numberFormat,
                            onItemClick = onNavigateToDetail,
                            onSuggestionClick = viewModel::send,
                            onRetry = viewModel::retryLast
                        )
                    }
                }
            }
            AssistantInputBar(
                input = uiState.input,
                thinking = uiState.thinking,
                onInputChange = viewModel::updateInput,
                onSend = { viewModel.send(uiState.input) },
                onInputFocused = { inputFocusTick++ },
                modifier = Modifier
                    .padding(
                        top = AppSpacing.sm,
                        bottom = inputBottomPadding
                    )
            )
        }
    }

    if (showClearDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("清空对话", fontWeight = FontWeight.SemiBold, color = TextPrimary())
            },
            text = {
                Text(
                    "将开启一段新对话；当前对话仍会保留在「历史沟通」里，可随时找回。物品数据不受影响。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = Primary(), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = TextSecondary())
                }
            }
        )
    }

    if (showHistoryDialog) {
        AssistantHistoryDialog(
            conversations = uiState.conversations,
            currentId = uiState.currentConversationId,
            onOpen = { id -> viewModel.openConversation(id); showHistoryDialog = false },
            onDelete = viewModel::deleteConversations,
            onNew = viewModel::startNewConversation,
            onDismiss = { showHistoryDialog = false },
        )
    }
}

/**
 * Tab 模式顶栏：与其它一级页同一套留白（statusBarsPadding + pageEdge），
 * 标题下方用副标题交代「本地几件 · 合计多少 · 是否接入模型」。
 */
@Composable
private fun AssistantTabHeader(
    subtitle: String,
    hasMessages: Boolean,
    backdrop: Backdrop,
    onClearChat: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = AppSpacing.pageEdge,
                end = AppSpacing.pageEdge,
                top = AppSpacing.md,
                bottom = AppSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "拾物助手",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary(),
                fontFamily = FredokaFont,
                maxLines = 1
            )
            Spacer(Modifier.height(AppSpacing.hairline))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextAuxiliary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasMessages) {
            ClearChatButton(backdrop, onClearChat)
            Spacer(Modifier.width(AppSpacing.xs))
        }
        HistoryButton(backdrop, onOpenHistory)
    }
}

@Composable
private fun ClearChatButton(backdrop: Backdrop, onClick: () -> Unit) {
    LiquidIconButton(onClick = onClick, backdrop = backdrop) {
        AppIcon(AppIcons.close, "清空对话", Modifier.size(19.dp), tint = TextAuxiliary())
    }
}

@Composable
private fun HistoryButton(backdrop: Backdrop, onClick: () -> Unit) {
    LiquidIconButton(onClick = onClick, backdrop = backdrop) {
        AppIcon(AppIcons.history, "历史沟通", Modifier.size(20.dp), tint = TextPrimary())
    }
}

/** 历史沟通：列表 + 多选删除；点条目直接打开会话。 */
@Composable
private fun AssistantHistoryDialog(
    conversations: List<ConversationEntity>,
    currentId: Long?,
    onOpen: (Long) -> Unit,
    onDelete: (Set<Long>) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA) }

    LiquidDialog(onDismissRequest = onDismiss, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "历史沟通",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary(),
                modifier = Modifier.weight(1f),
            )
            if (conversations.isNotEmpty()) {
                TextButton(onClick = {
                    selecting = !selecting
                    selected = emptySet()
                }) { Text(if (selecting) "取消" else "选择", color = Primary()) }
            }
        }
        if (conversations.isEmpty()) {
            Text(
                "还没有历史沟通，问一句就会自动保存。",
                style = MaterialTheme.typography.bodySmall,
                color = TextAuxiliary(),
                modifier = Modifier.padding(vertical = 28.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                items(conversations, key = { it.id }) { conv ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selecting) {
                                    selected = if (conv.id in selected) selected - conv.id else selected + conv.id
                                } else {
                                    onOpen(conv.id)
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selecting) {
                            Checkbox(
                                checked = conv.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + conv.id else selected - conv.id
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                conv.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                buildString {
                                    append(dateFormat.format(Date(conv.updatedAt)))
                                    if (conv.id == currentId) append(" · 当前会话")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextAuxiliary(),
                            )
                        }
                        if (!selecting) {
                            AppIcon(AppIcons.chevronRight, null, Modifier.size(14.dp), tint = TextAuxiliary())
                        }
                    }
                    HorizontalDivider(color = TextAuxiliary().copy(alpha = 0.10f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                onNew()
                onDismiss()
            }) { Text("新建对话", color = Primary()) }
            Spacer(Modifier.weight(1f))
            if (selecting) {
                TextButton(
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        onDelete(selected)
                        selected = emptySet()
                        selecting = false
                    },
                ) {
                    Text(
                        if (selected.isEmpty()) "删除选中" else "删除选中（${selected.size}）",
                        color = if (selected.isEmpty()) TextAuxiliary() else Color(0xFFE5484D),
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text("关闭", color = TextSecondary()) }
            }
        }
    }
}
