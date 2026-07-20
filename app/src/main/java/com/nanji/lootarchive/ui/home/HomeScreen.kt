package com.nanji.lootarchive.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.GlassStatCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import java.io.File
import java.text.NumberFormat

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

    // 数字滚动动画
    val animCount by animateIntAsState(uiState.totalCount, animationSpec = tween(400))
    val animValue by animateFloatAsState(uiState.totalValue.toFloat(), animationSpec = tween(400))
    val animWarranty by animateIntAsState(uiState.warrantyExpiringCount, animationSpec = tween(400))

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                kotlinx.coroutines.delay(800)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── 数据统计三卡片（横跨两列） ───
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassStatCard("物品总数", "$animCount", Modifier.weight(1f))
                    GlassStatCard("全部资产", "¥${numberFormat.format(animValue.toDouble())}",
                        Modifier.weight(1f), onClick = onNavigateToStats)
                    GlassStatCard("保修待提醒", "$animWarranty",
                        Modifier.weight(1f),
                        valueColor = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring else Primary(),
                        onClick = { if (uiState.warrantyExpiringCount > 0) showWarrantyDialog = true })
                }
            }

            // ─── 快捷功能横条（横跨两列） ───
            item(span = { GridItemSpan(2) }) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickAction("分类管理", Icons.Filled.Category, onNavigateToCategory)
                        QuickAction("导出Excel", Icons.Filled.FileDownload, onExportExcel)
                        QuickAction("导入Excel", Icons.Filled.UploadFile, onImportExcel)
                        QuickAction("备份数据", Icons.Filled.Backup, onBackupData)
                    }
                }
            }

            // ─── 物品双列网格 ───
            if (uiState.items.isEmpty() && !uiState.isLoading) {
                item(span = { GridItemSpan(2) }) {
                    EmptyState(
                        icon = { Icon(Icons.Outlined.Inventory2, null, Modifier.size(120.dp), tint = Color(0xFFBBBBBB)) },
                        title = "还没有物品",
                        subtitle = "点击「新增物品」开始记录你的第一件宝贝吧"
                    )
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
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
        val expiringItems = uiState.items.filter { it.warrantyExpiryDate != null && it.warrantyExpiryDate < System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 }
        AlertDialog(
            onDismissRequest = { showWarrantyDialog = false },
            modifier = Modifier.glassEffect(tier = GlassTier.DIALOG),
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

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, null, tint = Primary(), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary())
    }
}

@Composable
private fun ItemCard(item: ItemEntity, photoPath: String?, numberFormat: NumberFormat, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        // 缩略图（真实照片或占位）
        Surface(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            if (photoPath != null) {
                AsyncImage(model = File(photoPath), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(32.dp), tint = Color(0xFFBBBBBB))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, fontSize = 18.sp, color = TextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("¥${numberFormat.format(item.purchasePrice)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary())
            if (item.warrantyExpiryDate != null) {
                val days = (item.warrantyExpiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                val color = when { days < 0 -> WarrantyExpired; days <= 7 -> WarrantyExpiring; else -> TextAuxiliary() }
                val text = when { days < 0 -> "已过期"; days == 0L -> "今天到期"; else -> "${days}天" }
                Text(text, fontSize = 13.sp, color = color)
            }
        }
    }
}
