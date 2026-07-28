package com.nanji.lootarchive.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.StatCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.FormatUtil
import java.io.File
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categoryFilter: Pair<Long, String>? = null,
    onNavigateToAddItem: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToCategory: () -> Unit = {},
    onExportExcel: () -> Unit = {},
    onImportExcel: () -> Unit = {},
    onBackupData: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    var showWarrantyDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredItems = remember(uiState.items, categoryFilter) {
        if (categoryFilter != null) uiState.items.filter { it.categoryId == categoryFilter.first }
        else uiState.items
    }
    val filteredCount = filteredItems.size
    val filteredValue = filteredItems.sumOf { it.purchasePrice }

    val animCount by animateIntAsState(filteredCount, animationSpec = tween(500, easing = androidx.compose.animation.core.EaseOutCubic))
    val animValue by animateFloatAsState(filteredValue.toFloat(), animationSpec = tween(500, easing = androidx.compose.animation.core.EaseOutCubic))
    val animWarranty by animateIntAsState(uiState.warrantyExpiringCount, animationSpec = tween(500, easing = androidx.compose.animation.core.EaseOutCubic))

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch { delay(600); isRefreshing = false }
        },
        modifier = Modifier.fillMaxSize().background(Color.Transparent),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 68.dp, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 资产总览（独占一行） ──
            item(span = { GridItemSpan(2) }) {
                StatCard(
                    "全部资产", FormatUtil.formatPriceShort(animValue.toDouble()),
                    Modifier.fillMaxWidth(), onClick = onNavigateToStats
                )
            }
            // ── 物品总数 + 保修提醒（并排） ──
            item(span = { GridItemSpan(2) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("物品总数", "$animCount", Modifier.weight(1f))
                    StatCard(
                        "保修待提醒", "$animWarranty",
                        Modifier.weight(1f),
                        valueColor = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring else Primary(),
                        onClick = { if (uiState.warrantyExpiringCount > 0) showWarrantyDialog = true }
                    )
                }
            }

            // ── 骨架加载 ──
            if (uiState.isLoading && filteredItems.isEmpty()) {
                for (i in 1..6) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().height(200.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Box(Modifier.fillMaxWidth().height(130.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
                            Column(Modifier.padding(14.dp)) {
                                Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp)))
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp)))
                            }
                        }
                    }
                }
            }

            // ── 空状态 ──
            if (filteredItems.isEmpty() && !uiState.isLoading) {
                item(span = { GridItemSpan(2) }) {
                    EmptyState(
                        icon = { Icon(Icons.Outlined.Inventory2, null, Modifier.size(100.dp), tint = TextAuxiliary().copy(alpha = 0.5f)) },
                        title = "还没有物品",
                        subtitle = "点击下方 + 按钮记录你的第一件宝贝吧"
                    )
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        photoPath = uiState.photoPaths[item.id],
                        numberFormat = numberFormat,
                        onClick = { onNavigateToDetail(item.id) }
                    )
                }
            }
        }
    }

    // 保修待提醒弹窗
    if (showWarrantyDialog) {
        val expiringItems = uiState.items.filter {
            it.warrantyExpiryDate != null &&
            it.warrantyExpiryDate < System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
        }
        AlertDialog(
            onDismissRequest = { showWarrantyDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("保修待提醒 (${expiringItems.size})", fontWeight = FontWeight.SemiBold) },
            text = {
                if (expiringItems.isEmpty()) {
                    Text("暂无即将到期的保修物品")
                } else {
                    LazyColumn { items(expiringItems.size) { i -> Text("${expiringItems[i].name}", modifier = Modifier.padding(vertical = 4.dp)) } }
                }
            },
            confirmButton = { TextButton(onClick = { showWarrantyDialog = false }) { Text("关闭") } }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Claymorphism 物品卡片 — 22dp 大圆角 + 柔和阴影 + 价格 Pill
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ItemCard(
    item: ItemEntity,
    photoPath: String?,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val cardBg = if (dark) _CardDark else _CardLight
    val shadowColor = if (dark) Color.Black.copy(alpha = 0.25f) else Color(0xFFE0C8A0).copy(alpha = 0.18f)
    val catColor = ChartColors[(item.categoryId % ChartColors.size).toInt()]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), ambientColor = shadowColor, spotColor = shadowColor.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── 照片区 135dp ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                if (photoPath != null) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(44.dp), tint = catColor.copy(alpha = 0.4f))
                    }
                }

                // 类别色点（左上）
                Surface(
                    Modifier.padding(10.dp).size(10.dp).align(Alignment.TopStart),
                    RoundedCornerShape(5.dp), color = catColor
                ) {}

                // 保修徽章（右上）
                if (item.warrantyExpiryDate != null) {
                    val days = (item.warrantyExpiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                    val badgeColor = when { days < 0 -> WarrantyExpired; days <= 7 -> WarrantyExpiring; else -> WarrantyActive }
                    Surface(
                        Modifier.padding(10.dp).align(Alignment.TopEnd),
                        RoundedCornerShape(10.dp), color = badgeColor.copy(alpha = 0.88f)
                    ) {
                        Text(
                            when { days < 0 -> "过期"; days == 0L -> "今天"; else -> "${days}天" },
                            fontSize = 11.sp, color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ── 文字区 ──
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    item.name, fontSize = 16.sp, color = TextPrimary(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                // 价格 Pill 标签（白色底 + 蜜橘文字）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Primary().copy(alpha = 0.10f)
                    ) {
                        Text(
                            "¥${numberFormat.format(item.purchasePrice)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
