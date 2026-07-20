package com.nanji.lootarchive.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.ChartColors
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("物品统计") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.categorySummaries.isEmpty()) {
            EmptyState(
                icon = { Icon(Icons.Filled.BarChart, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                title = "暂无统计数据",
                subtitle = "添加物品后即可查看统计图表",
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 汇总数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard("物品总数", "${uiState.totalCount}", Modifier.weight(1f))
                    SummaryCard("总价值", "¥${numberFormat.format(uiState.totalValue)}", Modifier.weight(1f))
                }

                // 时间筛选
                GlassCard {
                    Text("时间筛选", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "全部", "3months" to "近3月", "6months" to "近半年", "1year" to "近1年").forEach { (value, label) ->
                            FilterChip(
                                selected = uiState.timeFilter == value,
                                onClick = { viewModel.setTimeFilter(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 饼图 — 各分类价值占比
                GlassCard {
                    Text("各分类价值占比", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    // 简易饼图（文字版）
                    uiState.categorySummaries.forEachIndexed { index, summary ->
                        val percentage = if (uiState.totalValue > 0)
                            (summary.totalValue / uiState.totalValue * 100).toInt() else 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = ChartColors[index % ChartColors.size]
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(summary.category.name, modifier = Modifier.weight(1f))
                            Text("¥${numberFormat.format(summary.totalValue)}", fontWeight = FontWeight.Medium)
                            Text("  $percentage%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 柱状图 — 各分类物品数量对比
                GlassCard {
                    Text("各分类物品数量对比", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    val maxCount = uiState.categorySummaries.maxOfOrNull { it.itemCount } ?: 1
                    uiState.categorySummaries.forEachIndexed { index, summary ->
                        val barWidth = if (maxCount > 0) (summary.itemCount.toFloat() / maxCount) else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                summary.category.name,
                                modifier = Modifier.width(64.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(barWidth.coerceIn(0f, 1f)),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                        color = ChartColors[index % ChartColors.size]
                                    ) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${summary.itemCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
