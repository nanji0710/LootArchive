package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.topFade
import com.nanji.lootarchive.ui.liquidglass.LiquidSegmentOption
import com.nanji.lootarchive.ui.liquidglass.LiquidSegmentedControl
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.component.GlassSurface
import com.nanji.lootarchive.ui.component.RadarAxis
import com.nanji.lootarchive.ui.component.RadarChart
import com.nanji.lootarchive.ui.component.TrendLineChart
import com.nanji.lootarchive.ui.component.TrendPoint
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nanji.lootarchive.util.FormatUtil
import java.text.NumberFormat
import kotlin.math.abs

private val CardPadding = 16.dp

/**
 * 模块标题 —— 六个模块统一的视觉起点：主题色图标胶囊 + 标题 + 右侧说明。
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
                Column(Modifier.fillMaxSize().verticalScroll(scrollState).topFade(scrollState).statusBarsPadding().navigationBarsPadding().padding(horizontal = AppSpacing.pageEdge, vertical = AppSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {

                // ── 资产总览 ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(18.dp).clip(AppShape.chip).background(Primary().copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(12.dp), tint = Primary())
                                    }
                                    Spacer(Modifier.width(AppSpacing.sm))
                                    Text("全部资产总值", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
                                }
                                Spacer(Modifier.height(AppSpacing.sm))
                                val totalText = "¥${numberFormat.format(uiState.totalValue)}"
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
                                        "本月新增 ${FormatUtil.formatPriceFull(uiState.monthPurchases)}",
                                        style = MaterialTheme.typography.labelSmall, color = TextSecondary(),
                                        fontFamily = FredokaFont, maxLines = 1
                                    )
                                    val prevMonth = uiState.prevMonthPurchases
                                    if (prevMonth > 0.0) {
                                        val diff = uiState.monthPurchases - prevMonth
                                        val tint = when { diff > 0.0 -> WarrantyActive; diff < 0.0 -> WarrantyExpired; else -> TextAuxiliary() }
                                        Spacer(Modifier.width(AppSpacing.sm))
                                        Text(
                                            buildString {
                                                append("较上月 ")
                                                if (diff > 0.0) append('+') else if (diff < 0.0) append('-')
                                                append(FormatUtil.formatPriceFull(abs(diff)))
                                                if (diff != 0.0) {
                                                    append("  ")
                                                    append(if (diff > 0.0) "↑" else "↓")
                                                    append(' ')
                                                    append("%.1f".format(abs(diff) / prevMonth * 100))
                                                    append('%')
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall, color = tint,
                                            fontWeight = FontWeight.SemiBold, fontFamily = FredokaFont, maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(AppSpacing.xl))
                            // 物品总数：原右列位置，改成一张内嵌小面板（参考图的右上角计数块）
                            Column(
                                Modifier.clip(AppShape.panel).background(TextAuxiliary().copy(alpha = 0.08f))
                                    .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Inventory2, null, Modifier.size(12.dp), tint = TextAuxiliary())
                                    Spacer(Modifier.width(AppSpacing.xs))
                                    Text("物品总数", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
                                }
                                Spacer(Modifier.height(AppSpacing.xs))
                                Text(
                                    numberFormat.format(uiState.totalCount),
                                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                    color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
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
                                selectedIndex = when (timeFilter) { "3months" -> 1; "6months" -> 2; "1year" -> 3; else -> 0 },
                                onSelected = { idx ->
                                    val key = listOf("all", "3months", "6months", "1year")[idx]
                                    timeFilter = key
                                    viewModel.setTimeFilter(key)
                                },
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

                // ── 分类资产分布 Donut ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        SectionHeader(Icons.Rounded.PieChart, "分类资产分布", "${uiState.categorySummaries.size} 个分类")
                        Spacer(Modifier.height(AppSpacing.xl))
                        val catTotal = uiState.categorySummaries.sumOf { it.totalValue }
                        val totalForPie = catTotal
                        Box(
                            Modifier.fillMaxWidth().height(172.dp)
                                // Canvas 图表对无障碍不可读，提供图例摘要供 TalkBack 朗读
                                .semantics {
                                    contentDescription = buildString {
                                        uiState.categorySummaries.forEachIndexed { _, s ->
                                            append(s.category.name)
                                            append(' ').append(percentLabel(s.totalValue, totalForPie))
                                            append('，')
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = TextAuxiliary().copy(alpha = 0.10f)
                            val sweeps = uiState.categorySummaries.map { if (totalForPie > 0) (it.totalValue / totalForPie * 360f).toFloat() else 0f }
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
                                            ChartColors[i % ChartColors.size],
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
                                val centerText = "¥${numberFormat.format(uiState.totalValue)}"
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
                        // 图例：色点 + 名称 + 金额 + 占比（占比固定列宽，小数点才对得齐）
                        uiState.categorySummaries.forEachIndexed { i, s ->
                            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(10.dp), AppShape.chip, color = ChartColors[i % ChartColors.size]) {}
                                Spacer(Modifier.width(AppSpacing.lg))
                                Text(s.category.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("¥${numberFormat.format(s.totalValue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Primary(), fontFamily = FredokaFont, maxLines = 1)
                                Spacer(Modifier.width(AppSpacing.lg))
                                Text(percentLabel(s.totalValue, totalForPie), style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, textAlign = TextAlign.End, modifier = Modifier.width(44.dp), maxLines = 1)
                            }
                        }
                    }
                }

                // ── v5.4 分类多维雷达图 ──
                val topSums = uiState.categorySummaries.take(5)
                if (topSums.size >= 2) {
                    val maxV = topSums.maxOf { it.totalValue }.toFloat().coerceAtLeast(1f)
                    val radarAxes = topSums.map { cs ->
                        RadarAxis(
                            label = cs.category.name.take(4),
                            value = cs.totalValue.toFloat(),
                            maxValue = maxV,
                            // 六位以上金额用缩写（¥1.2M），避免轴标签挤到画布外
                            valueLabel = if (cs.totalValue >= 100_000) FormatUtil.formatPriceShort(cs.totalValue) else FormatUtil.formatPriceFull(cs.totalValue)
                        )
                    }
                    GlassSurface {
                        Column(Modifier.padding(CardPadding)) {
                            SectionHeader(Icons.Rounded.Radar, "分类多维对比", "Top ${topSums.size}")
                            Spacer(Modifier.height(AppSpacing.md))
                            RadarChart(radarAxes, Modifier.fillMaxWidth(), sizeDp = 250f)
                        }
                    }
                }

                // ── v5.4 资产净值趋势折线图（累计购入额）──
                val tSdf = java.text.SimpleDateFormat("yy/MM", java.util.Locale.getDefault())
                val purchaseMap = uiState.items
                    .filter { it.purchaseDate != null }
                    .groupBy { tSdf.format(java.util.Date(it.purchaseDate!!)) }
                    .mapValues { it.value.sumOf { i -> i.purchasePrice } }
                val allTrendMonths = purchaseMap.keys.sorted()
                val trendPoints = if (allTrendMonths.size >= 2) {
                    var runningTotal = 0.0
                    allTrendMonths.map { m ->
                        runningTotal += purchaseMap[m] ?: 0.0
                        TrendPoint(m, runningTotal)
                    }
                } else emptyList()
                if (trendPoints.size >= 2) {
                    GlassSurface {
                        Column(Modifier.padding(CardPadding)) {
                            SectionHeader(Icons.AutoMirrored.Rounded.TrendingUp, "资产净值趋势", "${trendPoints.size} 个月")
                            Spacer(Modifier.height(AppSpacing.md))
                            TrendLineChart(trendPoints, Modifier.fillMaxWidth(), sizeDp = 210f)
                        }
                    }
                }

                // ── 月度购入趋势 Sparkline ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                        val purchaseByMonth = uiState.items.filter { it.purchaseDate != null }
                            .groupBy { sdf.format(java.util.Date(it.purchaseDate!!)) }
                            .mapValues { it.value.sumOf { i -> i.purchasePrice } }
                        val allMonths = purchaseByMonth.keys.sorted().takeLast(12)
                        val md = allMonths.map { m -> m to (purchaseByMonth[m] ?: 0.0) }
                        SectionHeader(Icons.Rounded.BarChart, "月度购入趋势", if (md.isEmpty()) null else "近 ${md.size} 个月")
                        Spacer(Modifier.height(AppSpacing.xl))
                        if (md.isEmpty()) Text("暂无购入数据", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary(), fontFamily = FredokaFont)
                        else {
                            val mv = md.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                            val maxMonth = md.maxByOrNull { it.second }?.first
                            val trackColor = TextAuxiliary().copy(alpha = 0.07f)
                            // 柱体：底轨 + 渐变填充（顶部浓、靠近底线淡）；当月金额最高的一根用主色强调
                            val bars: @Composable (Dp) -> Unit = { colW ->
                                md.forEachIndexed { i, pair ->
                                    val ym = pair.first
                                    val t = pair.second
                                    val isMax = ym == maxMonth && t > 0.0
                                    val barColor = if (isMax) Primary() else ChartColors[i % ChartColors.size]
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(colW)) {
                                        Text(
                                            FormatUtil.formatPriceShort(t),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMax) Primary() else TextAuxiliary(),
                                            fontWeight = if (isMax) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1, fontFamily = FredokaFont
                                        )
                                        Spacer(Modifier.height(AppSpacing.xs))
                                        Box(Modifier.width(26.dp).height(100.dp), contentAlignment = Alignment.BottomCenter) {
                                            Box(Modifier.fillMaxSize().clip(AppShape.bar).background(trackColor))
                                            Box(
                                                Modifier.fillMaxWidth()
                                                    .fillMaxHeight((t / mv).toFloat().coerceIn(0.04f, 1f))
                                                    .clip(AppShape.bar)
                                                    .background(Brush.verticalGradient(listOf(barColor.copy(alpha = 0.95f), barColor.copy(alpha = 0.55f))))
                                            )
                                        }
                                        Spacer(Modifier.height(AppSpacing.sm))
                                        val pts = ym.split("-")
                                        Text(
                                            if (pts.size == 2) "${pts[0].takeLast(2)}/${pts[1]}" else ym,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMax) TextSecondary() else TextAuxiliary(),
                                            fontWeight = if (isMax) FontWeight.SemiBold else FontWeight.Normal,
                                            fontFamily = FredokaFont, textAlign = TextAlign.Center, maxLines = 1
                                        )
                                    }
                                }
                            }
                            // 列宽自适应：≤6 列时平分整宽并居中；>6 列时每屏正好 6 列、横向滚动（不出现半截柱子）
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val visibleCols = if (md.size <= 6) md.size else 6
                                val colWidth = maxWidth / visibleCols.coerceAtLeast(1)
                                Row(
                                    Modifier.fillMaxWidth().height(160.dp)
                                        .then(if (md.size > 6) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                                    horizontalArrangement = if (md.size <= 6) Arrangement.Center else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) { bars(colWidth) }
                            }
                            Spacer(Modifier.height(AppSpacing.xs))
                            // 底线：给柱体一个落脚处
                            HorizontalDivider(color = TextAuxiliary().copy(alpha = 0.12f))
                        }
                    }
                }

                // ── 分类排名 ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        val rk = uiState.categorySummaries.sortedByDescending { it.totalValue }
                        SectionHeader(Icons.Rounded.Leaderboard, "分类排名", "共 ${rk.size} 类")
                        Spacer(Modifier.height(AppSpacing.md))
                        val topValue = rk.maxOfOrNull { it.totalValue }?.coerceAtLeast(1.0) ?: 1.0
                        rk.forEachIndexed { i, s ->
                            // 颜色与环形图一致：按分类在汇总列表里的位置取色，跨模块同色同义
                            val colorIdx = uiState.categorySummaries.indexOfFirst { it.category.id == s.category.id }.coerceAtLeast(0)
                            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(24.dp).clip(AppShape.chip).background(TextAuxiliary().copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextAuxiliary(), fontFamily = FredokaFont) }
                                Spacer(Modifier.width(AppSpacing.lg))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(s.category.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.width(AppSpacing.sm))
                                        Box(Modifier.clip(AppShape.pill).background(TextAuxiliary().copy(alpha = 0.10f)).padding(horizontal = AppSpacing.sm, vertical = 1.dp)) {
                                            Text("${s.itemCount} 件", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), fontFamily = FredokaFont, maxLines = 1)
                                        }
                                    }
                                    Spacer(Modifier.height(AppSpacing.sm))
                                    // 占比条：以榜首为满格
                                    Box(Modifier.fillMaxWidth().height(4.dp).clip(AppShape.bar).background(TextAuxiliary().copy(alpha = 0.08f))) {
                                        Box(
                                            Modifier.fillMaxWidth((s.totalValue / topValue).toFloat().coerceIn(0.04f, 1f))
                                                .fillMaxHeight().clip(AppShape.bar)
                                                .background(ChartColors[colorIdx % ChartColors.size])
                                        )
                                    }
                                }
                                Spacer(Modifier.width(AppSpacing.lg))
                                Text("¥${numberFormat.format(s.totalValue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Primary(), fontFamily = FredokaFont, maxLines = 1)
                            }
                            if (i < rk.size - 1) HorizontalDivider(Modifier.padding(start = 36.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                        }
                    }
                }
                Spacer(Modifier.height(AppSpacing.bottomBarClearance))
            }
            }
        }
    }
}
