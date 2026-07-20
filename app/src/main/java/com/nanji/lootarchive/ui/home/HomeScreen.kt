package com.nanji.lootarchive.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddItem: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = remember(uiState.currency) { getCurrencySymbol(uiState.currency) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { /* Flow 自动更新，下拉刷新触发重组 */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── 顶部导航区 ───
                item {
                    TopNavigationBar(
                        appName = uiState.appName,
                        onSearchClick = onNavigateToSearch,
                        onSettingsClick = onNavigateToSettings
                    )
                }

                // ─── 核心数据区 ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassStatCard(
                            title = "物品总数",
                            value = "${uiState.totalCount}",
                            modifier = Modifier.weight(1f)
                        )
                        GlassStatCard(
                            title = "总价值",
                            value = "${currencySymbol}${NumberFormat.getNumberInstance().format(uiState.totalValue)}",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToStatistics
                        )
                        GlassStatCard(
                            title = "待提醒",
                            value = "${uiState.warrantyExpiringCount}",
                            valueColor = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToStatistics
                        )
                    }
                }

                // ─── 功能快捷区 ───
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Filled.Add,
                                label = "新增物品",
                                onClick = onNavigateToAddItem
                            )
                            QuickActionButton(
                                icon = Icons.Filled.Category,
                                label = "分类管理",
                                onClick = onNavigateToCategory
                            )
                            QuickActionButton(
                                icon = Icons.Filled.FileDownload,
                                label = "导出Excel",
                                onClick = onNavigateToBackup
                            )
                            QuickActionButton(
                                icon = Icons.Filled.Backup,
                                label = "备份数据",
                                onClick = onNavigateToBackup
                            )
                        }
                    }
                }

                // ─── 物品列表区 ───
                item {
                    Text(
                        text = "物品列表",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                if (uiState.items.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(
                            icon = {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            title = "还没有物品",
                            subtitle = "点击「新增物品」开始记录你的第一件宝贝吧"
                        )
                    }
                } else {
                    items(uiState.items, key = { it.id }) { item ->
                        ItemListItem(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNavigationBar(
    appName: String,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Filled.Search, contentDescription = "搜索")
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, contentDescription = "设置")
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ItemListItem(
    item: ItemEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图占位（实际从照片表加载）
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currencySymbol}${NumberFormat.getNumberInstance().format(item.purchasePrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.storageLocation.isNotEmpty()) {
                        Text(
                            text = " · ${item.storageLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 保修状态
                if (item.warrantyExpiryDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val warrantyStatus = getWarrantyStatus(item.warrantyExpiryDate)
                    Text(
                        text = warrantyStatus.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = warrantyStatus.color
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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
    "USD" -> "$"
    "EUR" -> "€"
    "JPY" -> "¥"
    "GBP" -> "£"
    else -> "¥"
}
