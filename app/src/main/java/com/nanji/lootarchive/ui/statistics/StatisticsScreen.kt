package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.GlassSurface
import com.nanji.lootarchive.ui.component.RadarAxis
import com.nanji.lootarchive.ui.component.RadarChart
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.util.FormatUtil
import com.nanji.lootarchive.util.csvEscape
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit, onNavigateToDetail: (Long) -> Unit, isTabMode: Boolean = false,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    var timeFilter by remember { mutableStateOf("all") }
    DisposableEffect(Unit) { viewModel.refresh(); onDispose { } }

    Scaffold(topBar = { if (!isTabMode) TopAppBar(title = { Text("资产汇总", fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }) }, containerColor = Color.Transparent) { padding ->
        var refreshing by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; scope.launch { viewModel.refresh(); delay(600); refreshing = false } }, modifier = Modifier.fillMaxSize().padding(padding).background(Color.Transparent)) {
            if (uiState.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary()) }
            else if (uiState.categorySummaries.isEmpty()) EmptyState(icon = { Icon(Icons.Rounded.BarChart, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.4f)) }, title = "暂无统计数据", subtitle = "添加物品后即可查看统计图表")
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = CardPadding, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ── 资产总览 ──
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else Color(0xFFFFF8F0)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(Modifier.padding(CardPadding)) {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("全部资产总值", fontSize = 13.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                                Spacer(Modifier.height(4.dp))
                                Text("¥${numberFormat.format(uiState.totalValue)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("物品总数", fontSize = 13.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                                Spacer(Modifier.height(4.dp))
                                Text("${uiState.totalCount}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)) {
                            Row(Modifier.padding(3.dp)) {
                                listOf("all" to "全部", "3months" to "近三月", "6months" to "近半年", "1year" to "近一年").forEach { (k, l) ->
                                    val sel = timeFilter == k
                                    Surface(onClick = { timeFilter = k; viewModel.setTimeFilter(k) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (sel) Primary() else Color.Transparent) { Text(l, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) Color.White else TextSecondary(), modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center, fontFamily = FredokaFont) }
                                }
                            }
                        }
                        // v6.6 已出收益（仅>0时显示）
                        if (uiState.saleRevenue > 0) {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, Modifier.size(16.dp), tint = StatusSold)
                                Spacer(Modifier.width(6.dp))
                                Text("售出收益", fontSize = 13.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                                Spacer(Modifier.weight(1f))
                                Text("+¥${numberFormat.format(uiState.saleRevenue)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = StatusSold, fontFamily = MonoFont)
                            }
                        }
                    }
                }

                // ── 分类资产分布 Donut ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        Text("分类资产分布", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                        Spacer(Modifier.height(16.dp))
                        val catTotal = uiState.categorySummaries.sumOf { it.totalValue }
                            val totalForPie = catTotal + uiState.saleRevenue
                            Box(
                                Modifier.fillMaxWidth().height(160.dp)
                                    // Canvas 图表对无障碍不可读，提供图例摘要供 TalkBack 朗读
                                    .semantics {
                                        contentDescription = buildString {
                                            uiState.categorySummaries.forEachIndexed { i, s ->
                                                append(s.category.name)
                                                append(' ').append(if (totalForPie > 0) (s.totalValue / totalForPie * 100).toInt() else 0).append('%')
                                                append('，')
                                            }
                                            if (uiState.saleRevenue > 0) {
                                                append("售出收益")
                                                append(if (totalForPie > 0) (uiState.saleRevenue / totalForPie * 100).toInt() else 0).append('%')
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            val holeColor = CardBg()
                            val sweeps = uiState.categorySummaries.map { if (totalForPie > 0) (it.totalValue / totalForPie * 360f).toFloat() else 0f }.toMutableList()
                            if (uiState.saleRevenue > 0) sweeps.add((uiState.saleRevenue / totalForPie * 360f).toFloat())
                            Canvas(Modifier.size(140.dp)) { var sa = -90f; sweeps.forEachIndexed { i, sw -> if (sw > 0) { val c = if (uiState.saleRevenue > 0 && i == sweeps.lastIndex) StatusSold else ChartColors[i % ChartColors.size]; drawArc(c, sa, sw, true, Offset.Zero, Size(size.width, size.height)); sa += sw } }; drawCircle(holeColor, size.width * 0.28f) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(FormatUtil.formatPriceShort(uiState.totalValue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
                                Text("总资产", fontSize = 12.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        uiState.categorySummaries.forEachIndexed { i, s -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(10.dp), RoundedCornerShape(5.dp), color = ChartColors[i % ChartColors.size]) {}; Spacer(Modifier.width(10.dp)); Text(s.category.name, fontSize = 13.sp, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont); Text("¥${numberFormat.format(s.totalValue)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary(), fontFamily = FredokaFont); Spacer(Modifier.width(6.dp)); Text("${if (totalForPie > 0) (s.totalValue / totalForPie * 100).toInt() else 0}%", fontSize = 11.sp, color = TextAuxiliary(), fontFamily = FredokaFont) } }
                        if (uiState.saleRevenue > 0) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(10.dp), RoundedCornerShape(5.dp), color = StatusSold) {}; Spacer(Modifier.width(10.dp)); Text("售出收益", fontSize = 13.sp, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont); Text("¥${numberFormat.format(uiState.saleRevenue)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = StatusSold, fontFamily = FredokaFont); Spacer(Modifier.width(6.dp)); Text("${if (totalForPie > 0) (uiState.saleRevenue / totalForPie * 100).toInt() else 0}%", fontSize = 11.sp, color = TextAuxiliary(), fontFamily = FredokaFont) }
                        }
                    }
                }

                // ── v5.4 分类多维雷达图 ──
                val topSums = uiState.categorySummaries.take(5)
                if (topSums.size >= 2) {
                    val maxV = topSums.maxOf { it.totalValue }.toFloat().coerceAtLeast(1f)
                    val radarAxes = topSums.map { cs ->
                        RadarAxis(cs.category.name.take(4), cs.totalValue.toFloat(), maxV)
                    }
                    GlassSurface {
                        Column(Modifier.padding(CardPadding)) {
                            Text("分类多维对比", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                            Spacer(Modifier.height(12.dp))
                            RadarChart(radarAxes, Modifier.fillMaxWidth(), sizeDp = 240f)
                        }
                    }
                }

                // ── v5.4 资产净值趋势折线图（累计值，含售出收益）──
                val tSdf = java.text.SimpleDateFormat("yy/MM", java.util.Locale.getDefault())
                val purchaseMap = uiState.items
                    .filter { it.purchaseDate != null }
                    .groupBy { tSdf.format(java.util.Date(it.purchaseDate!!)) }
                    .mapValues { it.value.sumOf { i -> i.purchasePrice } }
                val saleMap = uiState.allItems
                    .filter { ItemStatus.fromCode(it.status) == ItemStatus.SOLD && it.saleDate != null && it.salePrice != null }
                    .groupBy { tSdf.format(java.util.Date(it.saleDate!!)) }
                    .mapValues { it.value.sumOf { i -> i.salePrice!! } }
                val allTrendMonths = (purchaseMap.keys + saleMap.keys).distinct().sorted()
                val trendPoints = if (allTrendMonths.size >= 2) {
                    var runningTotal = 0.0
                    allTrendMonths.map { m ->
                        runningTotal += (purchaseMap[m] ?: 0.0) + (saleMap[m] ?: 0.0)
                        com.nanji.lootarchive.ui.component.TrendPoint(m, runningTotal)
                    }
                } else emptyList()
                if (trendPoints.size >= 2) {
                    GlassSurface {
                        Column(Modifier.padding(CardPadding)) {
                            Text("资产净值趋势", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            com.nanji.lootarchive.ui.component.TrendLineChart(trendPoints, Modifier.fillMaxWidth(), sizeDp = 200f)
                        }
                    }
                }

                // ── 月度购入趋势 Sparkline ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        Text("月度购入趋势", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                        Spacer(Modifier.height(14.dp))
                        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                        val purchaseByMonth = uiState.items.filter { it.purchaseDate != null }
                            .groupBy { sdf.format(java.util.Date(it.purchaseDate!!)) }
                            .mapValues { it.value.sumOf { i -> i.purchasePrice } }
                        val saleByMonth = uiState.allItems.filter { ItemStatus.fromCode(it.status) == ItemStatus.SOLD && it.saleDate != null && it.salePrice != null }
                            .groupBy { sdf.format(java.util.Date(it.saleDate!!)) }
                            .mapValues { it.value.sumOf { i -> i.salePrice!! } }
                        val allMonths = (purchaseByMonth.keys + saleByMonth.keys).distinct().sorted().takeLast(12)
                        val md = allMonths.map { m -> m to ((purchaseByMonth[m] ?: 0.0) + (saleByMonth[m] ?: 0.0)) }
                        if (md.isEmpty()) Text("暂无购入数据", fontSize = 14.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                        else {
                            val mv = md.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                            val colW = 52.dp
                            // ≤6列居中无滚动，>6列横向滚动
                            if (md.size <= 6) {
                                Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                                    md.forEachIndexed { i, (ym, t) ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(colW)) {
                                            Text(FormatUtil.formatPriceShort(t), fontSize = 10.sp, color = TextAuxiliary(), maxLines = 1, fontFamily = FredokaFont)
                                            Spacer(Modifier.height(4.dp))
                                            Surface(Modifier.width(24.dp).height(((t / mv) * 100).dp.coerceAtLeast(4.dp)), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), color = ChartColors[i % ChartColors.size]) {}
                                            Spacer(Modifier.height(6.dp))
                                            val pts = ym.split("-")
                                            Text(if (pts.size == 2) "${pts[0].takeLast(2)}/${pts[1]}" else ym, fontSize = 10.sp, color = TextAuxiliary(), fontFamily = FredokaFont, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            } else {
                                Row(Modifier.fillMaxWidth().height(160.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Bottom) {
                                    md.forEachIndexed { i, (ym, t) ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(colW)) {
                                            Text(FormatUtil.formatPriceShort(t), fontSize = 10.sp, color = TextAuxiliary(), maxLines = 1, fontFamily = FredokaFont)
                                            Spacer(Modifier.height(4.dp))
                                            Surface(Modifier.width(24.dp).height(((t / mv) * 100).dp.coerceAtLeast(4.dp)), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), color = ChartColors[i % ChartColors.size]) {}
                                            Spacer(Modifier.height(6.dp))
                                            val pts = ym.split("-")
                                            Text(if (pts.size == 2) "${pts[0].takeLast(2)}/${pts[1]}" else ym, fontSize = 10.sp, color = TextAuxiliary(), fontFamily = FredokaFont, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 分类排名 ──
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        Text("分类排名", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                        Spacer(Modifier.height(12.dp))
                        val rk = uiState.categorySummaries.sortedByDescending { it.totalValue }
                        rk.forEachIndexed { i, s ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(24.dp), RoundedCornerShape(8.dp), color = when (i) { 0 -> Color(0xFFF59E0B); 1 -> Color(0xFFA8A29E); 2 -> Color(0xFFCDA87B); else -> Color.Transparent }) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (i < 3) Color.White else TextAuxiliary(), fontFamily = FredokaFont) } }
                                Spacer(Modifier.width(12.dp)); Text(s.category.name, fontSize = 14.sp, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
                                Text("¥${numberFormat.format(s.totalValue)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary(), fontFamily = FredokaFont)
                                Text("  ${s.itemCount}件", fontSize = 11.sp, color = TextAuxiliary(), fontFamily = FredokaFont)
                            }
                            if (i < rk.size - 1) HorizontalDivider(Modifier.padding(start = 36.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                        }
                    }
                }
                // ── v5.4 标签资产分布 ──
                val tagData = uiState.items
                    .flatMap { item -> item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it to item.purchasePrice } }
                    .groupBy { it.first }
                    .mapValues { e -> e.value.sumOf { it.second } }
                    .entries.sortedByDescending { it.value }.take(8)
                if (tagData.isNotEmpty()) {
                    GlassSurface {
                        Column(Modifier.padding(CardPadding)) {
                            Text("标签资产分布", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                            Spacer(Modifier.height(12.dp))
                            val maxTagVal = tagData.maxOf { it.value }.coerceAtLeast(1.0)
                            tagData.forEachIndexed { i, (tag, value) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(tag, fontSize = 12.sp, color = TextPrimary(), modifier = Modifier.width(56.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(TextAuxiliary().copy(alpha = 0.08f))) {
                                        Box(Modifier.fillMaxHeight().width(((value / maxTagVal) * 200).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(6.dp)).background(ChartColors[i % ChartColors.size]))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("¥${numberFormat.format(value)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Primary(), fontFamily = FredokaFont)
                                }
                            }
                        }
                    }
                }

                // ── v5.4 导出CSV ──
                val context = LocalContext.current
                GlassSurface {
                    Column(Modifier.padding(CardPadding)) {
                        Text("数据导出", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, maxLines = 1)
                        Spacer(Modifier.height(12.dp))
                        var showExportDone by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                try {
                                    val csv = buildString {
                                        appendLine("名称,分类,价格,位置,购入日期,状态,标签")
                                        uiState.items.forEach { item ->
                                            val cat = uiState.categorySummaries.find { it.category.id == item.categoryId }?.category?.name ?: "未知"
                                            val date = item.purchaseDate?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: ""
                                            appendLine(listOf(item.name, cat, item.purchasePrice.toString(), item.storageLocation, date, item.status, item.tags).joinToString(",") { csvEscape(it) })
                                        }
                                    }
                                    val fileName = "物品资产_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.csv"
                                    val file = java.io.File(context.cacheDir, fileName)
                                    file.writeText(csv, java.nio.charset.Charset.forName("UTF-8"))
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "导出CSV文件"))
                                    showExportDone = true
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary())
                        ) {
                            Icon(Icons.Rounded.FileDownload, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("导出CSV文件", fontWeight = FontWeight.Medium)
                        }
                        if (showExportDone) {
                            Spacer(Modifier.height(6.dp))
                            Text("CSV已生成，请选择分享方式", fontSize = 11.sp, color = Primary())
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}
