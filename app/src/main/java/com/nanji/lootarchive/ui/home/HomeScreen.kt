package com.nanji.lootarchive.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.GlassStatCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categoryFilter: Pair<Long, String>? = null,
    onNavigateToAddItem: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = remember(uiState.currency) { getCurrencySymbol(uiState.currency) }
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    // 应用分类筛选
    LaunchedEffect(categoryFilter) {
        // HomeViewModel 通过 flow 自动响应筛选
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { /* Flow 自动刷新 */ },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ─── 核心数据区（横跨两列） ───
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassStatCard(
                        title = "物品总数",
                        value = "${uiState.totalCount}",
                        modifier = Modifier.weight(1f)
                    )
                    GlassStatCard(
                        title = "总价值",
                        value = "${currencySymbol}${numberFormat.format(uiState.totalValue)}",
                        modifier = Modifier.weight(1f)
                    )
                    GlassStatCard(
                        title = "待提醒",
                        value = "${uiState.warrantyExpiringCount}",
                        valueColor = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── 快捷功能栏（横跨两列） ───
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionChip(
                            icon = Icons.Filled.Category,
                            label = "分类管理",
                            onClick = { /* 打开抽屉 */ }
                        )
                        QuickActionChip(
                            icon = Icons.Filled.FileDownload,
                            label = "导出Excel",
                            onClick = { /* TODO */ }
                        )
                        QuickActionChip(
                            icon = Icons.Filled.UploadFile,
                            label = "导入Excel",
                            onClick = { /* TODO */ }
                        )
                        QuickActionChip(
                            icon = Icons.Filled.Backup,
                            label = "备份数据",
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }

            // ─── 物品双列卡片 ───
            if (uiState.items.isEmpty() && !uiState.isLoading) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Outlined.Inventory2, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        title = "还没有物品",
                        subtitle = "点击右下角 + 按钮开始记录"
                    )
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
                    ItemGridCard(
                        item = item,
                        currencySymbol = currencySymbol,
                        numberFormat = numberFormat,
                        onClick = { onNavigateToDetail(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ItemGridCard(
    item: ItemEntity,
    currencySymbol: String,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            // 缩略图占位
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Image, null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                "${currencySymbol}${numberFormat.format(item.purchasePrice)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            // 保修状态标签
            if (item.warrantyExpiryDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val status = getWarrantyStatus(item.warrantyExpiryDate)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = status.color.copy(alpha = 0.12f)
                ) {
                    Text(
                        status.text,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = status.color
                    )
                }
            }
        }
    }
}

private data class WarrantyStatus(val text: String, val color: androidx.compose.ui.graphics.Color)

private fun getWarrantyStatus(expiryDate: Long): WarrantyStatus {
    val now = System.currentTimeMillis()
    val sevenDays = 7 * 24 * 60 * 60 * 1000L
    return when {
        expiryDate < now -> WarrantyStatus("已过期", WarrantyExpired)
        expiryDate <= now + sevenDays -> WarrantyStatus("即将到期", WarrantyExpiring)
        else -> WarrantyStatus("保修中", WarrantyActive)
    }
}

private fun getCurrencySymbol(code: String): String = when (code) {
    "USD" -> "$"; "EUR" -> "€"; "JPY" -> "¥"; "GBP" -> "£"; else -> "¥"
}
