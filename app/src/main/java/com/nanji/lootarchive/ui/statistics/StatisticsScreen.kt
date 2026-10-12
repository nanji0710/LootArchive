package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.GlassSurface
import com.nanji.lootarchive.ui.component.HistBucket
import com.nanji.lootarchive.ui.component.MonthlyBar
import com.nanji.lootarchive.ui.component.MonthlyBarChart
import com.nanji.lootarchive.ui.component.PriceHistogram
import com.nanji.lootarchive.ui.component.TrendLineChart
import com.nanji.lootarchive.ui.component.TrendPoint
import com.nanji.lootarchive.ui.component.topFade
import com.nanji.lootarchive.ui.liquidglass.LiquidSegmentOption
import com.nanji.lootarchive.ui.liquidglass.LiquidSegmentedControl
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.FormatUtil
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToLong

private val CardPadding = 16.dp

/** 时间筛选四档，索引即 LiquidSegmentedControl 的选中位。 */
private val TimeFilterKeys = listOf("all", "3months", "6months", "1year")

/**
 * 模块标题 —— 每个模块统一的视觉起点：主题色图标胶囊 + 标题 + 右侧说明。
 *
 * v7.1 起统计页每个模块都带这一行（参考图里每张卡都是「小图标 + 标题 + 右侧角标」）。
 * 图标底色用主题色的低透明度，不引入新色值。
 */
@Composable
private fun SectionHeader(icon: ImageVector, title: String, caption: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(AppShape.chip).background(Primary().copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(16.dp), tint = Primary()) }
        Spacer(Modifier.width(AppSpacing.md))
        Text(
            title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
            color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!caption.isNullOrBlank()) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
        }
    }
}

/** 占比文案：≥10% 取整，<10% 保留一位小数（0.9% / 0.2% 这类小分类才看得出差别）。 */
private fun percentLabel(part: Double, total: Double): String {
    if (total <= 0.0) return "0%"
    val p = part / total * 100.0
    return if (p >= 10.0) "${p.toInt()}%" else "%.1f%%".format(p)
}

/** `2026-09` → `26/09`，折线图横轴。 */
private fun trendLabel(yearMonth: String): String {
    val parts = yearMonth.split("-")
    return if (parts.size == 2) "${parts[0].takeLast(2)}/${parts[1]}" else yearMonth
}

/** `2026-09` → `09`，柱状图横轴（12 根柱子排不下年份）。 */
private fun monthLabel(yearMonth: String): String = yearMonth.substringAfterLast('-').take(2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit, onNavigateToDetail: (Long) -> Unit, isTabMode: Boolean = false,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    var timeFilter by remember { mutableStateOf("all") }

    Scaffold(topBar = { if (!isTabMode) TopAppBar(title = { Text("资产汇总", fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }) }, containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
            if (uiState.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary()) }
            else if (uiState.categorySummaries.isEmpty()) EmptyState(icon = { Icon(Icons.Rounded.BarChart, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.4f)) }, title = "暂无统计数据", subtitle = "添加物品后即可查看统计图表")
            else {
                // 滚动状态提到外面：顶栏渐隐要读它的滚动量（见 ui/component/ScrollFade.kt）
                val scrollState = rememberScrollState()
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollState).topFade(scrollState)
                        .statusBarsPadding().navigationBarsPadding()
                        .padding(horizontal = AppSpacing.pageEdge, vertical = AppSpacing.pageVertical),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                ) {
                    HeroCard(
                        state = uiState,
                        numberFormat = numberFormat,
                        selectedIndex = TimeFilterKeys.indexOf(timeFilter).coerceAtLeast(0),
                        onSelect = { idx ->
                            val key = TimeFilterKeys[idx]
                            timeFilter = key
                            viewModel.setTimeFilter(key)
                        }
                    )
                    StructureCard(uiState, numberFormat)
                    TrendCard(uiState)
                    MonthlyCard(uiState)
                    PriceHistogramCard(uiState)
                    TopValueCard(uiState)
                    LocationCard(uiState)
                    Spacer(Modifier.height(AppSpacing.bottomBarClearance))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  1. 资产总览 Hero
// ══════════════════════════════════════════════════════════════

@Composable
private fun HeroCard(
    state: StatisticsUiState,
    numberFormat: NumberFormat,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(18.dp).clip(AppShape.chip).background(Primary().copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(12.dp), tint = Primary()) }
                Spacer(Modifier.width(AppSpacing.sm))
                Text("全部资产总值", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
            }
            Spacer(Modifier.height(AppSpacing.md))
            val totalText = "¥${numberFormat.format(state.totalValue)}"
            Text(
                totalText,
                style = when (totalText.length) {
                    in 0..7 -> MaterialTheme.typography.displayMedium
                    in 8..10 -> MaterialTheme.typography.headlineLarge
                    in 11..13 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(AppSpacing.sm))
            // 本月新增 + 环比：口径与资产一致（只算拥有物品的购入价）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "本月新增 ${FormatUtil.formatPriceFull(state.monthPurchases)}",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary(),
                    fontFamily = FredokaFont, maxLines = 1
                )
                val prevMonth = state.prevMonthPurchases
                if (prevMonth > 0.0) {
                    Spacer(Modifier.width(AppSpacing.md))
                    DeltaChip(diff = state.monthPurchases - prevMonth, base = prevMonth)
                }
            }
            Spacer(Modifier.height(AppSpacing.lg))
            // 口径事实一条写清：件数 / 分类数 / 均价。不摆小方块 —— 要读的量都在下面几张图里。
            Text(
                buildAnnotatedString {
                    val num = SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary())
                    withStyle(num) { append("${state.totalCount}") }
                    append(" 件在持 · ")
                    withStyle(num) { append("${state.categorySummaries.size}") }
                    append(" 个分类 · 均价 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Primary())) {
                        // 均价一定带小数（总额除以件数），而 formatPriceFull 走的是
                        // 通用 NumberFormat（最多 3 位小数），直接传会印出「¥2,897.1」。
                        // 均价读到「元」就够了，先取整再格式化。
                        append(FormatUtil.formatPriceFull(state.avgPrice.roundToLong().toDouble()))
                    }
                },
                style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(),
                fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(AppSpacing.xl))
            // ── 液态玻璃时间筛选 ──
            val filterRowBrush = backgroundBrush()
            val filterBackdrop = rememberLayerBackdrop {
                drawRect(brush = filterRowBrush)
                drawContent()
            }
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .layerBackdrop(filterBackdrop)
                )
                LiquidSegmentedControl(
                    options = listOf(
                        LiquidSegmentOption("全部", Icons.Rounded.AllInclusive),
                        LiquidSegmentOption("近三月", Icons.Rounded.DateRange),
                        LiquidSegmentOption("近半年", Icons.Rounded.CalendarMonth),
                        LiquidSegmentOption("近一年", Icons.Rounded.CalendarToday)
                    ),
                    selectedIndex = selectedIndex,
                    onSelected = onSelect,
                    backdrop = filterBackdrop,
                    modifier = Modifier.fillMaxWidth(),
                    containerHeight = 40.dp,
                    contentPadding = 3.dp,
                    showIcons = false,
                    labelFontSize = 12.sp,
                    showSelectionShadow = false
                )
            }
        }
    }
}

/** 环比药丸：涨绿跌红，与保修状态同一套语义色。 */
@Composable
private fun DeltaChip(diff: Double, base: Double) {
    val up = diff >= 0
    val fg = if (up) WarrantyActive else WarrantyExpired
    val text = buildString {
        append(if (up) "↑ 较上月 +" else "↓ 较上月 -")
        append(FormatUtil.formatPriceFull(abs(diff)))
        append(" · ")
        append("%.1f".format(abs(diff) / base * 100))
        append('%')
    }
    Box(
        Modifier.clip(AppShape.pill).background(fg.copy(alpha = 0.14f))
            .padding(horizontal = AppSpacing.md, vertical = 2.dp)
    ) {
        Text(
            text, style = MaterialTheme.typography.labelSmall, color = fg,
            fontWeight = FontWeight.SemiBold, fontFamily = FredokaFont, maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ══════════════════════════════════════════════════════════════
//  2. 资产结构（环形 + 排名合并为一张卡）
// ══════════════════════════════════════════════════════════════

/**
 * 分类身份色：按「资产结构」降序榜的名次取色。
 *
 * 这样占比最高的分类拿到主题主色，不会因为分类表的插入顺序把主色分给一笔小钱；
 * 环形、排行、单品榜三处共用同一份映射，同色同义（不用 Composable 读色，Canvas 里也能调用）。
 */
private fun categoryPalette(summaries: List<CategorySummary>): Map<Long, Color> =
    summaries.sortedByDescending { it.totalValue }
        .mapIndexed { i, s -> s.category.id to ChartColors[i % ChartColors.size] }
        .toMap()

@Composable
private fun StructureCard(state: StatisticsUiState, numberFormat: NumberFormat) {
    val rows = state.categorySummaries.sortedByDescending { it.totalValue }
    if (rows.isEmpty()) return
    val total = state.totalValue
    val palette = categoryPalette(rows)
    // 兜底灰：金额为 0 已被上游过滤的分类不该冒充榜首色
    val fallbackColor = TextAuxiliary().copy(alpha = 0.35f)
    val colorOf: (Long) -> Color = { id -> palette[id] ?: fallbackColor }

    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(Icons.Rounded.PieChart, "资产结构", "${rows.size} 个分类")
            Spacer(Modifier.height(AppSpacing.xl))
            Box(
                Modifier.fillMaxWidth().height(172.dp)
                    // Canvas 图表对无障碍不可读，提供图例摘要供 TalkBack 朗读
                    .semantics {
                        contentDescription = buildString {
                            rows.forEach { s ->
                                append(s.category.name)
                                append(' ').append(s.itemCount).append(" 件")
                                append(' ').append(percentLabel(s.totalValue, total))
                                append('，')
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val trackColor = TextAuxiliary().copy(alpha = 0.10f)
                val sweeps = rows.map { if (total > 0) (it.totalValue / total * 360f).toFloat() else 0f }
                // 环形：底轨 + 分段圆弧（每段留一点缝，小分类也数得清）
                Canvas(Modifier.size(156.dp)) {
                    val ringW = size.width * 0.185f
                    val radius = (size.width - ringW) / 2f
                    val topLeft = Offset(ringW / 2f, ringW / 2f)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(ringW))
                    var startAngle = -90f
                    sweeps.forEachIndexed { i, sweep ->
                        if (sweep > 0f) {
                            val gap = minOf(1.6f, sweep * 0.12f)
                            drawArc(
                                colorOf(rows[i].category.id),
                                startAngle + gap / 2f,
                                (sweep - gap).coerceAtLeast(0.5f),
                                false, topLeft, arcSize,
                                style = Stroke(ringW, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val centerText = "¥${numberFormat.format(total)}"
                    Text(
                        centerText,
                        style = when (centerText.length) {
                            in 0..8 -> MaterialTheme.typography.titleMedium
                            in 9..11 -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont, maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("总资产", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont)
                }
            }
            Spacer(Modifier.height(AppSpacing.lg))
            // 排名与环形共用同一份降序列表，颜色按分类在汇总里的位置取，跨模块同色同义
            val topValue = rows.maxOf { it.totalValue }.coerceAtLeast(1.0)
            rows.forEachIndexed { i, s ->
                ShareRow(
                    rank = i + 1,
                    name = s.category.name,
                    countLabel = "${s.itemCount} 件",
                    amount = FormatUtil.formatPriceFull(s.totalValue),
                    percent = percentLabel(s.totalValue, total),
                    barFraction = (s.totalValue / topValue).toFloat().coerceIn(0.04f, 1f),
                    color = colorOf(s.category.id)
                )
                if (i < rows.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 30.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                }
            }
        }
    }
}

/**
 * 一行「名称 + 件数 + 份额条 + 金额（+ 占比）」。
 *
 * 份额条用 [Modifier.weight] 吃掉剩余宽度，所以名称长短不会挤掉金额：
 * 名称封顶 [nameMaxWidth]，条再短也不会把右侧数字推出卡片。
 */
@Composable
private fun ShareRow(
    rank: Int?,
    name: String,
    countLabel: String,
    amount: String,
    percent: String?,
    barFraction: Float,
    color: Color
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            val isTop = rank == 1
            val fg = if (isTop) Primary() else TextAuxiliary()
            Box(
                Modifier.size(22.dp).clip(AppShape.chip)
                    .background(fg.copy(alpha = if (isTop) 0.14f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = fg, fontFamily = FredokaFont
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
        }
        Text(
            name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary(),
            fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 76.dp)
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Box(
            Modifier.clip(AppShape.pill).background(TextAuxiliary().copy(alpha = 0.10f))
                .padding(horizontal = AppSpacing.sm, vertical = 1.dp)
        ) {
            Text(countLabel, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
        }
        Spacer(Modifier.width(AppSpacing.md))
        Box(
            Modifier.weight(1f).height(5.dp).clip(AppShape.pill)
                .background(TextAuxiliary().copy(alpha = 0.08f))
        ) {
            Box(
                Modifier.fillMaxWidth(barFraction).fillMaxHeight()
                    .clip(AppShape.pill).background(color)
            )
        }
        Spacer(Modifier.width(AppSpacing.md))
        Text(
            amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = Primary(), fontFamily = FredokaFont, maxLines = 1
        )
        if (percent != null) {
            Spacer(Modifier.width(AppSpacing.sm))
            Text(
                percent, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(),
                fontFamily = FredokaFont, textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp), maxLines = 1
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  3-4. 趋势与月度购入
// ══════════════════════════════════════════════════════════════

@Composable
private fun TrendCard(state: StatisticsUiState) {
    // v7.2：月份已由 ViewModel 补全成连续序列，x 轴才真正等距
    val points = state.cumulativeTrend.map { TrendPoint(trendLabel(it.yearMonth), it.value) }
    if (points.size < 2) return
    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(Icons.AutoMirrored.Rounded.TrendingUp, "资产净值趋势", "${points.size} 个月")
            Spacer(Modifier.height(AppSpacing.md))
            TrendLineChart(points, Modifier.fillMaxWidth(), sizeDp = 210f)
        }
    }
}

@Composable
private fun MonthlyCard(state: StatisticsUiState) {
    val months = state.monthlyPurchases
    if (months.isEmpty()) return
    val currentKey = months.lastOrNull()?.yearMonth
    val bars = months.map { MonthlyBar(monthLabel(it.yearMonth), it.value, it.yearMonth == currentKey) }
    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(
                Icons.Rounded.BarChart, "月度购入",
                "近 ${months.size} 个月 · 合计 ${FormatUtil.formatPriceShort(months.sumOf { it.value })}"
            )
            Spacer(Modifier.height(AppSpacing.xl))
            MonthlyBarChart(bars, Modifier.fillMaxWidth())
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  5. 客单价分布
// ══════════════════════════════════════════════════════════════

@Composable
private fun PriceHistogramCard(state: StatisticsUiState) {
    if (state.priceBuckets.isEmpty()) return
    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(Icons.Rounded.Payments, "客单价分布", "${state.totalCount} 件")
            Spacer(Modifier.height(AppSpacing.xl))
            PriceHistogram(
                state.priceBuckets.map { HistBucket(it.label, it.count) },
                Modifier.fillMaxWidth()
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  6. 单品价值 TOP 5
// ══════════════════════════════════════════════════════════════

@Composable
private fun TopValueCard(state: StatisticsUiState) {
    val items = state.topItems
    if (items.isEmpty()) return
    val topValue = items.sumOf { it.purchasePrice }
    val share = if (state.totalValue > 0.0) (topValue / state.totalValue * 100).toInt() else 0
    val maxPrice = items.maxOf { it.purchasePrice }.coerceAtLeast(1.0)
    val palette = categoryPalette(state.categorySummaries)
    val fallbackColor = TextAuxiliary().copy(alpha = 0.35f)

    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(Icons.Rounded.Leaderboard, "单品价值 TOP ${items.size}", "占资产 $share%")
            Spacer(Modifier.height(AppSpacing.md))
            items.forEachIndexed { i, item ->
                val isTop = i == 0
                val fg = if (isTop) Primary() else TextAuxiliary()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(22.dp).clip(AppShape.chip)
                            .background(fg.copy(alpha = if (isTop) 0.14f else 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = fg, fontFamily = FredokaFont
                        )
                    }
                    Spacer(Modifier.width(AppSpacing.md))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary(),
                                fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(AppSpacing.md))
                            Text(
                                FormatUtil.formatPriceFull(item.purchasePrice),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                color = Primary(), fontFamily = FredokaFont, maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(AppSpacing.sm))
                        // 条色用分类身份色：与「资产结构」同一份色板，跨卡同色同义
                        val barColor = palette[item.categoryId] ?: fallbackColor
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).clip(AppShape.bar)
                                .background(TextAuxiliary().copy(alpha = 0.08f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((item.purchasePrice / maxPrice).toFloat().coerceIn(0.04f, 1f))
                                    .fillMaxHeight().clip(AppShape.bar)
                                    .background(barColor)
                            )
                        }
                    }
                    state.categorySummaries
                        .firstOrNull { it.category.id == item.categoryId }
                        ?.let { summary ->
                            Spacer(Modifier.width(AppSpacing.md))
                            Box(
                                Modifier.clip(AppShape.pill).background(TextAuxiliary().copy(alpha = 0.10f))
                                    .padding(horizontal = AppSpacing.sm, vertical = 1.dp)
                            ) {
                                Text(
                                    summary.category.name, style = MaterialTheme.typography.labelSmall,
                                    color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1
                                )
                            }
                        }
                }
                if (i < items.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 30.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  7. 存放位置分布
// ══════════════════════════════════════════════════════════════

@Composable
private fun LocationCard(state: StatisticsUiState) {
    val rows = state.locationSummaries
    if (rows.isEmpty()) return
    val maxValue = rows.maxOf { it.totalValue }.coerceAtLeast(1.0)
    GlassSurface {
        Column(Modifier.padding(CardPadding)) {
            SectionHeader(Icons.Rounded.Place, "存放位置分布", "${rows.size} 处")
            Spacer(Modifier.height(AppSpacing.md))
            rows.forEachIndexed { i, row ->
                ShareRow(
                    rank = null,
                    name = row.name,
                    countLabel = "${row.itemCount} 件",
                    amount = FormatUtil.formatPriceFull(row.totalValue),
                    percent = null,
                    barFraction = (row.totalValue / maxValue).toFloat().coerceIn(0.04f, 1f),
                    color = ChartColors[i % ChartColors.size]
                )
                if (i < rows.lastIndex) {
                    HorizontalDivider(color = TextAuxiliary().copy(alpha = 0.10f))
                }
            }
        }
    }
}
