package com.nanji.lootarchive.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.data.assistant.AnswerBlock
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.ui.liquidglass.LiquidFilterChip
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.DateUtil
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
//  助理页的视觉积木：欢迎卡 / 消息气泡 / 结构化块 / 输入栏
//
//  ⚠ 液态玻璃组件的图层红线（v7.2.1 渲染线程递归崩溃的历史事故）：
//  玻璃件必须放在「它自己不在其中」的捕获层上采样 —— 要么采样只画了背景刷的
//  局部 layerBackdrop，要么像输入栏这样把内容画进局部层、按钮作为兄弟节点叠上去。
//  绝对不要直接把 LocalPageBackdrop（包含整页内容、也包含按钮自己）传给它们。
// ═══════════════════════════════════════════════════════════════

/** 欢迎页的快捷提问：覆盖六种高频意图，点一下就等于发一句话。 */
private val QuickQuestions = listOf(
    "我的东西一共值多少钱",
    "有多少件物品",
    "哪个分类最贵",
    "有哪些保修快到期了",
    "最近添加了什么",
    "怎么省钱",
    "新增物品：牛奶 20元"
)

/** 用户气泡最大宽度：留出右侧呼吸感，长句子自动换行而不是铺满整行。 */
private val BubbleMaxWidth = 300.dp

/** 输入行右侧给玻璃发送按钮让出的宽度（按钮叠在输入行之上，不参与输入行布局）。 */
private val SendButtonZoneWidth = 96.dp

/** 欢迎空态：品牌卡 + 快捷提问。 */
@Composable
internal fun AssistantWelcome(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = backgroundBrush()
    // 玻璃 chip 需要一层「不含自己」的捕获层：只画背景刷 + 本层内容（本层内容为空）。
    // 与首页分类 chips 的做法一致（见 HomeScreen 的 chipsBackdrop）。
    val chipsBackdrop = rememberLayerBackdrop {
        drawRect(brush = brush)
        drawContent()
    }
    Box(modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().layerBackdrop(chipsBackdrop))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.pageEdge, vertical = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.sheet,
                colors = CardDefaults.cardColors(containerColor = CardBg()),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xxxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = AppShape.card,
                        color = Primary().copy(alpha = 0.12f)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppIcon(AppIcons.assistant, null, Modifier.size(32.dp), tint = Primary())
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.xl))
                    Text(
                        "拾物助手",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary(),
                        fontFamily = FredokaFont
                    )
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        "问我任何关于你物品的事：值多少钱、放哪了、保修还剩多久。全部在本机算，不联网也能用。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(AppSpacing.xxl))
                    Text("试试这样问", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
                    Spacer(Modifier.height(AppSpacing.md))
                    QuickQuestionChips(backdrop = chipsBackdrop, onSend = onSend)
                }
            }
        }
    }
}

/** 快捷提问胶囊（FlowRow 自动换行，避免长问句被硬截断在小屏上）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickQuestionChips(
    backdrop: Backdrop,
    onSend: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        QuickQuestions.forEach { question ->
            LiquidFilterChip(
                selected = false,
                onClick = { onSend(question) },
                backdrop = backdrop,
                label = question,
                height = 34.dp
            )
        }
    }
}

/** 一条消息：用户右对齐、助手左对齐。 */
@Composable
internal fun AssistantMessageRow(
    message: ChatMessage,
    numberFormat: NumberFormat,
    onItemClick: (Long) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    if (message.fromUser) {
        UserBubble(message.text)
    } else {
        AssistantBubble(message, numberFormat, onItemClick, onSuggestionClick, onRetry)
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = BubbleMaxWidth)
                .clip(AppShape.panel)
                .background(Primary().copy(alpha = 0.12f))
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg)
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary())
        }
    }
}

@Composable
private fun AssistantBubble(
    message: ChatMessage,
    numberFormat: NumberFormat,
    onItemClick: (Long) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = AppShape.chip,
            color = Primary().copy(alpha = 0.12f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppIcon(AppIcons.assistant, null, Modifier.size(17.dp), tint = Primary())
            }
        }
        Spacer(Modifier.width(AppSpacing.md))
        Card(
            modifier = Modifier.weight(1f),
            shape = AppShape.card,
            colors = CardDefaults.cardColors(containerColor = CardBg()),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(AppSpacing.xl)) {
                if (message.pending) {
                    ThinkingIndicator()
                } else {
                    if (message.fromModel) {
                        Text(
                            "模型回答",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary(),
                            modifier = Modifier.padding(bottom = AppSpacing.sm),
                        )
                    }
                    if (message.reasoning.isNotBlank()) {
                        ReasoningSection(message.reasoning)
                        Spacer(Modifier.height(AppSpacing.md))
                    }
                    if (message.text.isNotBlank()) {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary())
                    }
                    message.blocks.forEach { block ->
                        Spacer(Modifier.height(AppSpacing.lg))
                        AnswerBlockContent(block, numberFormat, onItemClick)
                    }
                    if (message.error) {
                        Spacer(Modifier.height(AppSpacing.md))
                        LocalFallbackRow(onRetry)
                    }
                    if (message.suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(AppSpacing.lg))
                        SuggestionChips(message.suggestions, onSuggestionClick)
                    }
                }
            }
        }
    }
}

/**
 * 模型的思考过程（reasoning_content）：默认收起，点一下展开。
 * 长度可能很长，所以放在气泡顶部的浅色折叠块里，不打断正文阅读。
 */
@Composable
private fun ReasoningSection(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShape.panel)
            .background(Primary().copy(alpha = 0.06f))
            .clickable { expanded = !expanded }
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("思考过程", style = MaterialTheme.typography.labelSmall, color = Primary())
            Spacer(Modifier.weight(1f))
            Text(
                if (expanded) "收起" else "展开",
                style = MaterialTheme.typography.labelSmall,
                color = TextAuxiliary(),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(240), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200), shrinkTowards = Alignment.Top) + fadeOut(tween(140)),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = TextAuxiliary(),
                modifier = Modifier.padding(top = AppSpacing.sm),
            )
        }
    }
}

/**
 * 「思考中…」三点动画。
 *
 * 减弱动效开启时退化成静止的三个半透明点（不跑无限动画，也不闪）。
 */
@Composable
private fun ThinkingIndicator() {
    val reduceMotion = LocalReduceMotion.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("思考中", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary())
        Spacer(Modifier.width(AppSpacing.sm))
        if (reduceMotion) {
            repeat(3) { ThinkingDot(0.45f) }
        } else {
            val transition = rememberInfiniteTransition(label = "thinking")
            // 单一相位驱动三个点：不用三条独立动画，相位差就是「波纹」的来源
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "thinkingPhase"
            )
            repeat(3) { index ->
                ThinkingDot(alpha = dotAlpha(phase, index / 3f))
            }
        }
    }
}

/** 相位差 1/3 的呼吸曲线，落在 0.25~1.0 之间，避免点彻底消失。 */
private fun dotAlpha(phase: Float, offset: Float): Float {
    val wave = (sin(((phase + offset) % 1f) * 2f * PI.toFloat()) + 1f) / 2f
    return 0.25f + 0.75f * wave
}

@Composable
private fun ThinkingDot(alpha: Float) {
    Box(
        Modifier
            .padding(horizontal = 1.5.dp)
            .size(6.dp)
            .clip(AppShape.pill)
            .background(TextAuxiliary().copy(alpha = alpha.coerceIn(0.2f, 1f)))
    )
}

/** 模型失败时的说明行 + 重试入口。 */
@Composable
private fun LocalFallbackRow(onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "这次是用本地规则算的",
            style = MaterialTheme.typography.labelSmall,
            color = TextAuxiliary(),
            modifier = Modifier.weight(1f)
        )
        Text(
            "重试",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Primary(),
            modifier = Modifier
                .clip(AppShape.pill)
                .clickable(onClick = onRetry)
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
        )
    }
}

/** 「接着问」建议：普通胶囊（不在气泡里再开玻璃捕获层，避免滚动列表出现大量图层）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(suggestions: List<String>, onClick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        suggestions.forEach { suggestion ->
            Box(
                Modifier
                    .clip(AppShape.pill)
                    .background(Primary().copy(alpha = 0.10f))
                    .clickable { onClick(suggestion) }
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
            ) {
                Text(
                    suggestion,
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 结构化块：按 kind 选排版。 */
@Composable
private fun AnswerBlockContent(
    block: AnswerBlock,
    numberFormat: NumberFormat,
    onItemClick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (block.title.isNotBlank()) {
            Text(
                block.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary(),
                fontFamily = FredokaFont
            )
            Spacer(Modifier.height(AppSpacing.sm))
        }
        when (block.kind) {
            AnswerBlock.Kind.SUMMARY -> SummaryMetrics(block.lines)
            AnswerBlock.Kind.LIST -> block.items.forEachIndexed { index, item ->
                ItemResultRow(item, numberFormat) { onItemClick(item.id) }
                if (index != block.items.lastIndex) {
                    HorizontalDivider(color = TextAuxiliary().copy(alpha = 0.10f))
                }
            }
            AnswerBlock.Kind.TIP -> TipLines(block.lines)
            AnswerBlock.Kind.TEXT, AnswerBlock.Kind.UNKNOWN -> TextLines(block.lines)
        }
    }
}

/**
 * SUMMARY 的两列小指标。
 *
 * 引擎把摘要行编码成 `标签|数值`（见 AnswerBlock.Kind.SUMMARY 注释）；
 * 没有竖线的行当作一个整行标签，兼容以后手写的新块。
 */
@Composable
private fun SummaryMetrics(lines: List<String>) {
    val pairs = lines.map { line ->
        val separator = line.indexOf('|')
        if (separator >= 0) {
            line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        } else {
            "" to line.trim()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        pairs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                row.forEach { (label, value) ->
                    MetricCell(label, value, Modifier.weight(1f))
                }
                // 单数个指标时右侧留空，保持两列等宽对齐
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(AppShape.panel)
            .background(Primary().copy(alpha = 0.07f))
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary(),
            fontFamily = FredokaFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (label.isNotBlank()) {
            Spacer(Modifier.height(AppSpacing.hairline))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), maxLines = 1)
        }
    }
}

@Composable
private fun TipLines(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(AppShape.pill)
                        .background(Primary().copy(alpha = 0.55f))
                )
                Spacer(Modifier.width(AppSpacing.md))
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TextLines(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        lines.forEach { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, color = TextSecondary())
        }
    }
}

/** 回答里的物品条目：整行可点，进详情页。 */
@Composable
private fun ItemResultRow(
    item: ItemEntity,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.thumb)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(AppShape.pill)
                .background(statusColor(item.status))
        )
        Spacer(Modifier.width(AppSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(AppSpacing.hairline))
            Text(
                itemCaption(item, numberFormat),
                style = MaterialTheme.typography.labelSmall,
                color = TextAuxiliary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(AppSpacing.sm))
        AppIcon(AppIcons.chevronRight, null, Modifier.size(14.dp), tint = TextAuxiliary())
    }
}

/** 条目副标题：价格 · 状态 · 位置 · 保修剩余（只拼有信息的项）。 */
private fun itemCaption(item: ItemEntity, numberFormat: NumberFormat): String = buildList {
    add("¥" + numberFormat.format(item.purchasePrice))
    val status = ItemStatus.fromCode(item.status)
    if (status != ItemStatus.ACTIVE) add(status.label)
    if (item.storageLocation.isNotBlank()) add(item.storageLocation)
    item.warrantyExpiryDate?.let { add("保修" + DateUtil.formatRemainingDays(it)) }
}.joinToString(" · ")

/**
 * 底部输入栏：兜底快捷入口 + 玻璃卡片输入框 + 玻璃发送按钮。
 *
 * 图层关系：输入行在 [barBackdrop] 里（底色 + 输入文字），发送按钮是它的**兄弟节点**，
 * 采样这一层 → 按钮能看到「卡片底 + 文字」，却不会采样到自己。
 */
@Composable
internal fun AssistantInputBar(
    input: String,
    thinking: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onInputFocused: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val glassBg = LocalGlassColors.current.glassBg
    val barBackdrop = rememberLayerBackdrop {
        drawRect(glassBg)
        drawContent()
    }
    val canSend = input.isNotBlank() && !thinking

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.pageEdge)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                // 输入条按规范用 panel 圆角：比标准卡片更收，读起来像一个输入框而不是一张卡
                shape = AppShape.panel,
                colors = CardDefaults.cardColors(containerColor = glassBg),
                // 去掉默认海拔：半透明玻璃底 + 阴影会在浅色背景上显出一圈灰边（用户反馈的“阴影问题”）
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layerBackdrop(barBackdrop)
                        .heightIn(min = 48.dp)
                        // 右侧给玻璃发送按钮让出位置（按钮叠在这一行上面，不属于本图层）
                        .padding(
                            start = AppSpacing.xl,
                            end = SendButtonZoneWidth,
                            top = AppSpacing.sm,
                            bottom = AppSpacing.sm
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) onInputFocused() },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary()),
                        cursorBrush = SolidColor(Primary()),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (input.isEmpty()) {
                                    Text(
                                        "问问你的物品…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextAuxiliary()
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
            LiquidGlassButton(
                onClick = onSend,
                backdrop = barBackdrop,
                enabled = canSend,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = AppSpacing.md),
                height = 38.dp,
                horizontalPadding = AppSpacing.lg
            ) {
                val tint = if (canSend) Primary() else TextAuxiliary()
                AppIcon(AppIcons.send, "发送", Modifier.size(16.dp), tint = tint)
                Spacer(Modifier.width(AppSpacing.xs))
                Text(
                    "发送",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canSend) TextPrimary() else TextAuxiliary()
                )
            }
        }
    }
}
