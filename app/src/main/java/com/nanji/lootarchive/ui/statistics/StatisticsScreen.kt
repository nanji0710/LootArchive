package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.FormatUtil
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    isTabMode: Boolean = false,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    Scaffold(
        topBar = {
            if (!isTabMode) {
                TopAppBar(
                    title = { Text("资产汇总", fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "返回")
                        }
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true; scope.launch { delay(600); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.Transparent)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary())
                }
            } else if (uiState.categorySummaries.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Filled.BarChart, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.4f)) },
                    title = "暂无统计数据",
                    subtitle = "添加物品后即可查看统计图表"
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── v5.0 资产总览 Hero Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (LocalDarkTheme.current) _CardDark else Color(0xFFFFF8F0)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(20.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("物品总件数", fontSize = 14.sp, color = TextAuxiliary())
                                Spacer(Modifier.height(4.dp))
                                Text("${uiState.totalCount}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("全部资产总价", fontSize = 14.sp, color = TextAuxiliary())
                                Spacer(Modifier.height(4.dp))
                                Text("¥${numberFormat.format(uiState.totalValue)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
                            }
                        }
                    }

                    // ── v5.0 Donut 环形图 + 分类概览 ──
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Text("分类资产概览", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                        Spacer(Modifier.height(16.dp))

                        // Donut Chart
                        Box(
                            Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val holeColor = if (LocalDarkTheme.current) _CardDark else _CardLight
                            val sweepAngles = uiState.categorySummaries.map { summary ->
                                if (uiState.totalValue > 0) (summary.totalValue / uiState.totalValue * 360f).toFloat() else 0f
                            }
                            Canvas(Modifier.size(140.dp)) {
                                var startAngle = -90f
                                sweepAngles.forEachIndexed { index, sweep ->
                                    if (sweep > 0) {
                                        drawArc(
                                            color = ChartColors[index % ChartColors.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = true,
                                            size = Size(size.width, size.height)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                // Center hole
                                drawCircle(
                                    color = holeColor,
                                    radius = size.width * 0.28f
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "¥${FormatUtil.formatPriceShort(uiState.totalValue)}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = Primary(), fontFamily = FredokaFont
                                )
                                Text("总资产", fontSize = 10.sp, color = TextAuxiliary())
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Legend
                        uiState.categorySummaries.forEachIndexed { index, s ->
                            val pct = if (uiState.totalValue > 0) (s.totalValue / uiState.totalValue * 100).toInt() else 0
                            val barColor = ChartColors[index % ChartColors.size]
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(Modifier.size(10.dp), RoundedCornerShape(5.dp), color = barColor) {}
                                Spacer(Modifier.width(10.dp))
                                Text(s.category.name, fontSize = 13.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                                Text("¥${numberFormat.format(s.totalValue)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary())
                                Spacer(Modifier.width(6.dp))
                                Text("$pct%", fontSize = 11.sp, color = TextAuxiliary())
                            }
                        }
                    }

                    // ── v5.0 月度购入趋势 Sparkline ──
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Text("月度购入趋势", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                        Spacer(Modifier.height(14.dp))
                        val monthlyData = uiState.items.filter { it.purchaseDate != null }
                            .groupBy {
                                java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                                    .format(java.util.Date(it.purchaseDate!!))
                            }
                            .mapValues { it.value.sumOf { item -> item.purchasePrice } }
                            .toList().sortedBy { it.first }.takeLast(12)
                        if (monthlyData.isEmpty()) {
                            Text("暂无购入数据", fontSize = 14.sp, color = TextAuxiliary())
                        } else {
                            val maxVal = monthlyData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                            Row(
                                Modifier.fillMaxWidth().height(160.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                monthlyData.forEachIndexed { index, (ym, total) ->
                                    val barHeight = ((total / maxVal) * 120).dp.coerceAtLeast(4.dp)
                                    val barColor = ChartColors[index % ChartColors.size]
                                    val parts = ym.split("-")
                                    val label = if (parts.size == 2) "${parts[0].takeLast(2)}-${parts[1]}" else ym
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Text(FormatUtil.formatPriceShort(total), fontSize = 8.sp, color = TextAuxiliary(), maxLines = 1)
                                        Spacer(Modifier.height(3.dp))
                                        Surface(
                                            Modifier.width(18.dp).height(barHeight),
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                            color = barColor
                                        ) {}
                                        Spacer(Modifier.height(4.dp))
                                        Text(label, fontSize = 8.sp, color = TextAuxiliary(), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // ── v5.0 分类排行榜 ──
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Text("分类排名", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                        Spacer(Modifier.height(12.dp))
                        val ranked = uiState.categorySummaries.sortedByDescending { it.totalValue }
                        ranked.forEachIndexed { index, s ->
                            val rankBg = when (index) {
                                0 -> Color(0xFFF59E0B)
                                1 -> Color(0xFFA8A29E)
                                2 -> Color(0xFFD4A574)
                                else -> Color.Transparent
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (index < 3) {
                                    Surface(
                                        Modifier.size(22.dp), RoundedCornerShape(8.dp), color = rankBg
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                } else {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 12.sp,
                                        color = TextAuxiliary(),
                                        modifier = Modifier.width(22.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(s.category.name, fontSize = 14.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                                Text(
                                    "¥${numberFormat.format(s.totalValue)}",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary(),
                                    fontFamily = FredokaFont
                                )
                                Text(
                                    "  ${s.itemCount}件",
                                    fontSize = 11.sp, color = TextAuxiliary()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
