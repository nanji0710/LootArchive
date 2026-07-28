package com.nanji.lootarchive.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.StatCard
import com.nanji.lootarchive.ui.component.HeroStatCard
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

    // v5.0: Category chips state
    val catViewModel: com.nanji.lootarchive.ui.component.CategoryDrawerViewModel = hiltViewModel()
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

    // v5.0: 在 @Composable 顶层计算 categoryCount
    val categoryCount = catState.categories.size

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch { delay(600); isRefreshing = false }
        },
        modifier = Modifier.fillMaxSize().background(Color.Transparent)
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 68.dp, bottom = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            // ── v5.0 Hero 资产总览 (独占一行) ──
            item(span = StaggeredGridItemSpan.FullLine) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(
                                brush = if (LocalDarkTheme.current)
                                    Brush.linearGradient(listOf(_CardDark, _CardDark))
                                else
                                    Brush.linearGradient(listOf(Color(0xFFFFF8F0), Color(0xFFFFF0E0)))
                            )
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("全部资产", fontSize = 13.sp, color = TextAuxiliary())
                            Spacer(Modifier.height(4.dp))
                            Text(
                                FormatUtil.formatPriceShort(animValue.toDouble()),
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = Primary(), fontFamily = FredokaFont
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroStatCard(
                                title = "物品总数",
                                value = "$animCount",
                                modifier = Modifier.weight(1f),
                                valueColor = TextPrimary()
                            )
                            HeroStatCard(
                                title = "保修待提醒",
                                value = "$animWarranty",
                                modifier = Modifier.weight(1f),
                                valueColor = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring else Primary(),
                                accentBg = if (uiState.warrantyExpiringCount > 0) WarrantyExpiring.copy(alpha = 0.10f) else null,
                                onClick = { if (uiState.warrantyExpiringCount > 0) showWarrantyDialog = true }
                            )
                            HeroStatCard(
                                title = "分类数",
                                value = "$categoryCount",
                                modifier = Modifier.weight(1f),
                                valueColor = Secondary(),
                                accentBg = Secondary().copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
            }

            // ── v5.0 水平分类胶囊 (FullLine) ──
            item(span = StaggeredGridItemSpan.FullLine) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = effectiveFilter == null,
                            onClick = { chipFilter = null },
                            label = { Text("全部", fontSize = 13.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(catState.categories.size) { idx ->
                        val cat = catState.categories[idx]
                        val selected = effectiveFilter?.first == cat.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                chipFilter = if (selected) null else Pair(cat.id, cat.name)
                            },
                            label = { Text(cat.name, fontSize = 13.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // ── v5.0 最近添加标题 ──
            if (filteredItems.isNotEmpty() && !uiState.isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        "最近添加",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary(),
                        fontFamily = FredokaFont,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── 骨架加载 ──
            if (uiState.isLoading && filteredItems.isEmpty()) {
                for (i in 1..6) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().height(200.dp),
                            shape = RoundedCornerShape(20.dp),
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
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptyState(
                        icon = { Icon(Icons.Outlined.Inventory2, null, Modifier.size(100.dp), tint = TextAuxiliary().copy(alpha = 0.4f)) },
                        title = "还没有物品",
                        subtitle = "点击下方按钮记录你的第一件宝贝吧",
                        actionLabel = "添加第一件",
                        onAction = onNavigateToAddItem
                    )
                }
            } else {
                // ── v5.0 Bento Grid: 高价值物品 FullLine, 其余 1列 ──
                items(
                    count = filteredItems.size,
                    key = { filteredItems[it].id },
                    span = { index ->
                        val item = filteredItems[index]
                        if (item.purchasePrice >= 1000 && index % 5 == 0)
                            StaggeredGridItemSpan.FullLine
                        else
                            StaggeredGridItemSpan.SingleLane
                    }
                ) { index ->
                    val item = filteredItems[index]
                    val isWide = item.purchasePrice >= 1000 && index % 5 == 0
                    ItemCard(
                        item = item,
                        photoPath = uiState.photoPaths[item.id],
                        numberFormat = numberFormat,
                        isWide = isWide,
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
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("保修待提醒 (${expiringItems.size})", fontWeight = FontWeight.SemiBold, color = TextPrimary()) },
            text = {
                if (expiringItems.isEmpty()) {
                    Text("暂无即将到期的保修物品", color = TextSecondary())
                } else {
                    LazyColumn {
                        items(expiringItems.size) { i ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(8.dp), RoundedCornerShape(4.dp), color = WarrantyExpiring) {}
                                Spacer(Modifier.width(10.dp))
                                Text(expiringItems[i].name, fontSize = 14.sp, color = TextPrimary())
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWarrantyDialog = false }) { Text("关闭", color = Primary()) } }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  v5.0 Bento 物品卡片
//  isWide=true: 使用 StaggeredGridItemSpan.FullLine (2列宽)
//  isWide=false: 使用普通1列卡片
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ItemCard(
    item: ItemEntity,
    photoPath: String?,
    numberFormat: NumberFormat,
    isWide: Boolean,
    onClick: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val cardBg = if (dark) _CardDark else _CardLight
    val shadowColor = if (dark) Color.Black.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.05f)
    val catColor = ChartColors[(item.categoryId % ChartColors.size).toInt()]

    val photoHeight = if (isWide) 160.dp else 140.dp
    val nameSize = if (isWide) 16.sp else 15.sp
    val priceSize = if (isWide) 18.sp else 16.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = shadowColor, spotColor = shadowColor.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── 照片区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(photoHeight)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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
                        Modifier.fillMaxSize().background(catColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(if (isWide) 52.dp else 44.dp), tint = catColor.copy(alpha = 0.35f))
                    }
                }

                // 类别色点
                Surface(
                    Modifier.padding(10.dp).size(10.dp).align(Alignment.TopStart),
                    RoundedCornerShape(5.dp), color = catColor
                ) {}

                // 保修徽章
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
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // NEW 角标
                val isRecent = System.currentTimeMillis() - item.createdAt < 7 * 24 * 60 * 60 * 1000
                if (isRecent && item.warrantyExpiryDate == null) {
                    Surface(
                        Modifier.padding(10.dp).align(Alignment.TopStart),
                        RoundedCornerShape(6.dp),
                        color = Primary().copy(alpha = 0.85f)
                    ) {
                        Text(
                            "NEW",
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // ── 文字区 ──
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    item.name, fontSize = nameSize, color = TextPrimary(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary().copy(alpha = 0.12f)
                ) {
                    Text(
                        "¥${numberFormat.format(item.purchasePrice)}",
                        fontSize = priceSize,
                        fontWeight = FontWeight.Bold,
                        color = Primary(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
