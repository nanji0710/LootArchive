package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    isTabMode: Boolean = false,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    Scaffold(
        topBar = { if (!isTabMode) { TopAppBar(title = { Text("资产汇总") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } }) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.categorySummaries.isEmpty()) {
            EmptyState(
                icon = { Icon(Icons.Filled.BarChart, null, Modifier.size(100.dp), tint = Color(0xFFBBBBBB)) },
                title = "暂无统计数据",
                subtitle = "添加物品后即可查看统计图表",
                modifier = Modifier.padding(padding)
            )
        } else {
            var refreshing by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshing = true; scope.launch { kotlinx.coroutines.delay(800); refreshing = false } }
            ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── 资产总览卡片 ───
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("物品总件数", fontSize = 16.sp, color = TextSecondary())
                            Spacer(Modifier.height(4.dp))
                            Text("${uiState.totalCount}", fontSize = 24.sp, color = TextPrimary())
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("全部资产总价", fontSize = 16.sp, color = TextSecondary())
                            Spacer(Modifier.height(4.dp))
                            Text("¥${numberFormat.format(uiState.totalValue)}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Primary())
                        }
                    }
                }

                // ─── 饼图卡片 ───
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("分类资产价值占比", fontSize = 18.sp, color = TextPrimary())
                    Spacer(Modifier.height(16.dp))
                    uiState.categorySummaries.forEachIndexed { index, s ->
                        val pct = if (uiState.totalValue > 0) (s.totalValue / uiState.totalValue * 100).toInt() else 0
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(10.dp), RoundedCornerShape(5.dp), color = ChartColors[index % ChartColors.size]) {}
                            Spacer(Modifier.width(8.dp))
                            Text(s.category.name, fontSize = 13.sp, color = TextSecondary(), modifier = Modifier.weight(1f))
                            Text("¥${numberFormat.format(s.totalValue)}", fontSize = 14.sp, color = Primary())
                            Spacer(Modifier.width(8.dp))
                            Text("$pct%", fontSize = 13.sp, color = TextAuxiliary())
                        }
                    }
                }

                // ─── 柱状图卡片 ───
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("分类物品数量对比", fontSize = 18.sp, color = TextPrimary())
                    Spacer(Modifier.height(16.dp))
                    val maxCount = uiState.categorySummaries.maxOfOrNull { it.itemCount }?.coerceAtLeast(1) ?: 1
                    uiState.categorySummaries.forEachIndexed { index, s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.category.name, fontSize = 13.sp, color = TextSecondary(), modifier = Modifier.width(64.dp))
                            Surface(Modifier.weight(1f).height(24.dp), RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(Modifier.fillMaxSize()) {
                                    Surface(Modifier.fillMaxHeight().fillMaxWidth((s.itemCount.toFloat() / maxCount).coerceIn(0f, 1f)), RoundedCornerShape(4.dp), color = ChartColors[index % ChartColors.size]) {}
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${s.itemCount}", fontSize = 14.sp, color = TextPrimary())
                        }
                    }
                }

                // ─── 分类明细表 ───
                Text("分类明细", fontSize = 18.sp, color = TextPrimary(), modifier = Modifier.padding(top = 4.dp))
                uiState.categorySummaries.forEach { s ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.width(4.dp).height(32.dp), RoundedCornerShape(2.dp), color = ChartColors[uiState.categorySummaries.indexOf(s) % ChartColors.size]) {}
                            Spacer(Modifier.width(12.dp))
                            Text(s.category.name, fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                            Text("${s.itemCount}件", fontSize = 13.sp, color = TextSecondary())
                            Spacer(Modifier.width(12.dp))
                            Text("¥${numberFormat.format(s.totalValue)}", fontSize = 14.sp, color = Primary())
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            } // PullToRefreshBox
        }
    }
}
