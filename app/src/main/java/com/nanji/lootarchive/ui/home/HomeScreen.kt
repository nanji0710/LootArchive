package com.nanji.lootarchive.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel
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

    val catViewModel: CategoryDrawerViewModel = hiltViewModel()
    val catState by catViewModel.uiState.collectAsState()
    var chipFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val effectiveFilter = categoryFilter ?: chipFilter

    val filteredItems = remember(uiState.items, effectiveFilter) {
        if (effectiveFilter != null) uiState.items.filter { it.categoryId == effectiveFilter.first }
        else uiState.items
    }
    val filteredCount = filteredItems.size
    val filteredValue = filteredItems.sumOf { it.purchasePrice }

    val animCount by animateIntAsState(filteredCount, animationSpec = tween(600, easing = androidx.compose.animation.core.EaseOutCubic))
    val animValue by animateFloatAsState(filteredValue.toFloat(), animationSpec = tween(600, easing = androidx.compose.animation.core.EaseOutCubic))
    val animWarranty by animateIntAsState(uiState.warrantyExpiringCount, animationSpec = tween(600, easing = androidx.compose.animation.core.EaseOutCubic))

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; scope.launch { delay(600); isRefreshing = false } }, modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 68.dp, bottom = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 对标 HTML .ph-hero: 全部资产 大数字 + 3个小统计 ──
            item(span = { GridItemSpan(2) }) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (LocalDarkTheme.current) _CardDark else Color(0xFFFFF8F0)).padding(20.dp)) {
                    Column {
                        Text("全部资产", fontSize = 13.sp, color = TextAuxiliary())
                        Spacer(Modifier.height(4.dp))
                        Text(FormatUtil.formatPriceShort(animValue.toDouble()), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Primary().copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$animCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                                    Text("物品总数", fontSize = 11.sp, color = TextAuxiliary())
                                }
                            }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (uiState.warrantyExpiringCount > 0) WarrantyExpiring.copy(alpha = 0.10f) else Primary().copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 10.dp).clickable { if (uiState.warrantyExpiringCount > 0) showWarrantyDialog = true }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$animWarranty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (uiState.warrantyExpiringCount > 0) Color(0xFFEF4444) else Primary(), fontFamily = FredokaFont)
                                    Text("保修待提醒", fontSize = 11.sp, color = TextAuxiliary())
                                }
                            }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Secondary().copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${catState.categories.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Secondary(), fontFamily = FredokaFont)
                                    Text("分类数", fontSize = 11.sp, color = TextAuxiliary())
                                }
                            }
                        }
                    }
                }
            }

            // ── 对标 HTML .ph-chips: 水平圆角胶囊 ──
            item(span = { GridItemSpan(2) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    item {
                        FilterChip(selected = effectiveFilter == null, onClick = { chipFilter = null }, label = { Text("全部", fontSize = 13.sp) }, shape = RoundedCornerShape(20.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary(), selectedLabelColor = Color.White))
                    }
                    items(catState.categories.size) { idx ->
                        val cat = catState.categories[idx]
                        val sel = effectiveFilter?.first == cat.id
                        FilterChip(selected = sel, onClick = { chipFilter = if (sel) null else Pair(cat.id, cat.name) }, label = { Text(cat.name, fontSize = 13.sp) }, shape = RoundedCornerShape(20.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary(), selectedLabelColor = Color.White))
                    }
                }
            }

            // ── 对标 HTML .ph-section-title: "最近添加" ──
            if (filteredItems.isNotEmpty() && !uiState.isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Text("最近添加", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            // 骨架加载
            if (uiState.isLoading && filteredItems.isEmpty()) {
                for (i in 1..6) { item { Card(Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))) { Box(Modifier.fillMaxWidth().height(130.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))); Column(Modifier.padding(14.dp)) { Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))); Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth(0.4f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))) } } } }
            }

            // 空状态
            if (filteredItems.isEmpty() && !uiState.isLoading) {
                item(span = { GridItemSpan(2) }) {
                    EmptyState(icon = { Icon(Icons.Outlined.Inventory2, null, Modifier.size(100.dp), tint = TextAuxiliary().copy(alpha = 0.4f)) }, title = "还没有物品", subtitle = "点击下方按钮记录你的第一件宝贝吧", actionLabel = "添加第一件", onAction = onNavigateToAddItem)
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    val catColor = ChartColors[(item.categoryId % ChartColors.size).toInt()]
                    // 对标 HTML .bento-card: 20dp圆角 + 玻璃边框 + 微阴影
                    Card(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable { onNavigateToDetail(item.id) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(135.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                                if (uiState.photoPaths[item.id] != null) {
                                    AsyncImage(model = File(uiState.photoPaths[item.id]!!), contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.fillMaxSize().background(catColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Image, null, Modifier.size(44.dp), tint = catColor.copy(alpha = 0.35f)) }
                                }
                                // 类别色点 (左上)
                                Surface(Modifier.padding(10.dp).size(10.dp).align(Alignment.TopStart), RoundedCornerShape(5.dp), color = catColor) {}
                                // 保修徽章 (右上)
                                if (item.warrantyExpiryDate != null) {
                                    val days = (item.warrantyExpiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                                    val badgeColor = when { days < 0 -> WarrantyExpired; days <= 7 -> WarrantyExpiring; else -> WarrantyActive }
                                    Surface(Modifier.padding(10.dp).align(Alignment.TopEnd), RoundedCornerShape(10.dp), color = badgeColor.copy(alpha = 0.88f)) {
                                        Text(when { days < 0 -> "过期"; days == 0L -> "今天"; else -> "${days}天" }, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                // NEW 角标
                                if (System.currentTimeMillis() - item.createdAt < 7 * 24 * 60 * 60 * 1000 && item.warrantyExpiryDate == null) {
                                    Surface(Modifier.padding(10.dp).align(Alignment.TopStart), RoundedCornerShape(6.dp), color = Primary().copy(alpha = 0.85f)) {
                                        Text("NEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(item.name, fontSize = 15.sp, color = TextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Surface(shape = RoundedCornerShape(10.dp), color = Primary().copy(alpha = 0.12f)) {
                                    Text("¥${numberFormat.format(item.purchasePrice)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWarrantyDialog) {
        val expiringItems = uiState.items.filter { it.warrantyExpiryDate != null && it.warrantyExpiryDate < System.currentTimeMillis() + uiState.warrantyReminderDays * 24L * 60 * 60 * 1000 }.sortedBy { it.warrantyExpiryDate }
        val count = expiringItems.size
        AlertDialog(onDismissRequest = { showWarrantyDialog = false }, shape = RoundedCornerShape(28.dp), containerColor = MaterialTheme.colorScheme.surface, title = { Text("保修待提醒 ($count)", fontWeight = FontWeight.SemiBold, color = TextPrimary()) }, text = {
            if (count == 0) Text("暂无即将到期的保修物品", color = TextSecondary())
            else LazyColumn { items(expiringItems) { i -> Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(8.dp), RoundedCornerShape(4.dp), color = WarrantyExpiring) {}; Spacer(Modifier.width(10.dp)); Text(i.name, fontSize = 14.sp, color = TextPrimary()) } } }
        }, confirmButton = { TextButton(onClick = { showWarrantyDialog = false }) { Text("关闭", color = Primary()) } })
    }
}
	}
