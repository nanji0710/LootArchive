package com.nanji.lootarchive.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import java.io.File
import java.text.NumberFormat
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddItem: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    val focusRequester = remember { FocusRequester() }
    var showScopeRow by remember { mutableStateOf(false) }
    var showStatusRow by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagRow by remember { mutableStateOf(false) }
    val scopeLabels = remember { mapOf(null to "全部范围", "name" to "名称", "location" to "位置", "desc" to "备注", "warranty" to "保修") }
    val statusLabels = remember { mapOf(null to "全部状态", "active" to "在用", "idle" to "闲置", "sold" to "已出", "repair" to "待修", "lost" to "丢失") }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            // 搜索栏
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { // 默认 48dp 触摸目标
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", modifier = Modifier.size(22.dp), tint = TextPrimary())
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.08f) else Color.White,
                    shadowElevation = 2.dp
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = { Text("搜索物品名称 / 存放位置...", fontSize = 14.sp, color = TextAuxiliary()) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextAuxiliary(), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(Icons.Rounded.Close, "清除", Modifier.size(18.dp), tint = TextAuxiliary())
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // v6.0 折叠式筛选行
            val activeFilterCount = listOfNotNull(
                uiState.activeFilter,
                uiState.statusFilter,
                uiState.tagFilter
            ).size
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索范围（行内展开，与标签一致）
                val scopeLabel = scopeLabels[uiState.activeFilter] ?: "范围"
                FilterChip(
                    selected = uiState.activeFilter != null,
                    onClick = { showScopeRow = !showScopeRow },
                    label = { Text(scopeLabel, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(14.dp)) },
                    trailingIcon = {
                        Icon(
                            if (showScopeRow) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null, Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary().copy(alpha = 0.2f),
                        selectedLabelColor = Primary()
                    )
                )

                // 状态（行内展开，与标签一致）
                val statusLabel = statusLabels[uiState.statusFilter] ?: "状态"
                FilterChip(
                    selected = uiState.statusFilter != null,
                    onClick = { showStatusRow = !showStatusRow },
                    label = { Text(statusLabel, fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            if (showStatusRow) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null, Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary().copy(alpha = 0.2f),
                        selectedLabelColor = Primary()
                    )
                )

                // 标签切换（有标签时才显示）
                if (uiState.allTags.isNotEmpty()) {
                    val tagLabel = uiState.tagFilter ?: "标签"
                    FilterChip(
                        selected = uiState.tagFilter != null,
                        onClick = { showTagRow = !showTagRow },
                        label = { Text(tagLabel, fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(
                                if (showTagRow) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null, Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary().copy(alpha = 0.2f),
                            selectedLabelColor = Primary()
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                // 激活筛选计数
                if (activeFilterCount > 0) {
                    Text("$activeFilterCount 项筛选", fontSize = 11.sp, color = Primary())
                }
            }

            // 范围快捷选择行
            if (showScopeRow) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scopeLabels.forEach { (k, v) ->
                        val sel = uiState.activeFilter == k
                        FilterChip(
                            selected = sel,
                            onClick = { viewModel.setActiveFilter(k) },
                            label = { Text(v, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary().copy(alpha = 0.2f),
                                selectedLabelColor = Primary()
                            )
                        )
                    }
                }
            }

            // 状态快捷选择行
            if (showStatusRow) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusLabels.forEach { (k, v) ->
                        val sel = uiState.statusFilter == k
                        FilterChip(
                            selected = sel,
                            onClick = { viewModel.setStatusFilter(k) },
                            label = { Text(v, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary().copy(alpha = 0.2f),
                                selectedLabelColor = Primary()
                            )
                        )
                    }
                }
            }

            // 标签快捷选择（点击标签Chip展开/折叠）
            if (showTagRow && uiState.allTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.tagFilter == null,
                        onClick = { viewModel.setTagFilter(null) },
                        label = { Text("全部", fontSize = 11.sp) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary().copy(alpha = 0.15f),
                            selectedLabelColor = Primary()
                        )
                    )
                    uiState.allTags.take(6).forEach { tag ->
                        val sel = uiState.tagFilter == tag
                        FilterChip(
                            selected = sel,
                            onClick = { viewModel.setTagFilter(if (sel) null else tag) },
                            label = { Text(tag, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary().copy(alpha = 0.2f),
                                selectedLabelColor = Primary()
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 结果统计 + 排序
            if (uiState.query.isNotEmpty() || uiState.activeFilter != null || uiState.statusFilter != null || uiState.tagFilter != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("找到 ${uiState.results.size} 件物品", fontSize = 13.sp, color = TextPrimary(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Rounded.SwapVert, null, Modifier.size(14.dp), tint = Primary())
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (uiState.sort) {
                                    "price_desc" -> "价格↓"
                                    "date_new" -> "最新"
                                    "warranty" -> "保修"
                                    else -> "排序"
                                },
                                fontSize = 12.sp, color = Primary()
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu, onDismissRequest = { showSortMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            listOf(
                                "price_desc" to "价格从高到低",
                                "date_new" to "购入时间最新",
                                "warranty" to "保修到期优先"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontWeight = if (uiState.sort == key) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { viewModel.setSort(key); showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // 搜索结果 / 搜索历史 / 空状态
            if (uiState.results.isEmpty() && uiState.query.isEmpty() && uiState.activeFilter == null && uiState.statusFilter == null && uiState.tagFilter == null) {
                if (uiState.recentSearches.isNotEmpty()) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("最近搜索", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("清空", fontSize = 13.sp, color = Primary())
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.recentSearches.take(8).forEach { query ->
                                Surface(
                                    onClick = { viewModel.updateQuery(query); viewModel.submitSearch() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.History, null, Modifier.size(14.dp), tint = TextAuxiliary())
                                        Spacer(Modifier.width(10.dp))
                                        Text(query, fontSize = 14.sp, color = TextPrimary())
                                    }
                                }
                            }
                        }
                    }
                } else {
                    EmptyState(
                        icon = { Icon(Icons.Rounded.Search, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                        title = "搜索物品",
                        subtitle = "输入关键词查找你的物品"
                    )
                }
            } else if (uiState.results.isEmpty()) {
                if (uiState.query.isNotEmpty()) {
                    EmptyState(
                        icon = { Icon(Icons.Rounded.SearchOff, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                        title = "未找到对应物品",
                        subtitle = "换个关键词试试"
                    )
                } else {
                    EmptyState(
                        icon = { Icon(Icons.Rounded.Search, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                        title = "搜索你的物品",
                        subtitle = "输入关键词查找",
                        actionLabel = "添加第一件物品",
                        onAction = onNavigateToAddItem
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results, key = { it.item.id }) { result ->
                        SearchItemCard(result.item, result.firstPhotoPath, numberFormat) { onNavigateToDetail(result.item.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchItemCard(item: ItemEntity, firstPhotoPath: String?, numberFormat: NumberFormat, onClick: () -> Unit) {
    ClayCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp))) {
            if (firstPhotoPath != null) {
                AsyncImage(model = File(firstPhotoPath), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Primary().copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(36.dp), tint = TextAuxiliary().copy(alpha = 0.35f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, fontSize = 16.sp, color = TextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("¥${numberFormat.format(item.purchasePrice)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
    }
}
