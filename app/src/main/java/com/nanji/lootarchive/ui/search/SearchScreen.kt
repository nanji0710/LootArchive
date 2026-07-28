package com.nanji.lootarchive.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.*
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
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            // v5.0 搜索栏 — 圆角玻璃风格
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.ArrowBack, "返回", modifier = Modifier.size(22.dp), tint = TextPrimary())
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

            // v5.0 快捷筛选胶囊
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val filters = listOf("全部" to null, "名称" to "name", "位置" to "location", "备注" to "desc", "保修" to "warranty")
                items(filters.size) { index ->
                    val (label, _) = filters[index]
                    val selected = uiState.activeFilter == filters[index].second
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setActiveFilter(filters[index].second) },
                        label = { Text(label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(18.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary(),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 结果统计 + 排序
            if (uiState.query.isNotEmpty() || uiState.activeFilter != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("找到 ${uiState.results.size} 件物品", fontSize = 14.sp, color = TextPrimary(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    var showSort by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showSort = true }) {
                            Icon(Icons.Rounded.SwapVert, null, Modifier.size(16.dp), tint = Primary())
                            Spacer(Modifier.width(4.dp))
                            Text("排序", fontSize = 13.sp, color = Primary())
                        }
                        DropdownMenu(
                            expanded = showSort, onDismissRequest = { showSort = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            listOf(
                                "price_desc" to "价格从高到低",
                                "date_new" to "购入时间最新",
                                "warranty" to "保修到期优先"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSort(key); showSort = false })
                            }
                        }
                    }
                }
            }

            // v5.0 搜索结果 / 搜索历史 / 空状态
            if (uiState.results.isEmpty() && uiState.query.isEmpty() && uiState.activeFilter == null) {
                if (uiState.recentSearches.isNotEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("最近搜索", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("清空", fontSize = 13.sp, color = Primary())
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.recentSearches.take(6).forEach { query ->
                                Surface(
                                    onClick = { viewModel.updateQuery(query); viewModel.submitSearch() },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.History, null, Modifier.size(13.dp), tint = TextAuxiliary())
                                        Spacer(Modifier.width(6.dp))
                                        Text(query, fontSize = 13.sp, color = TextSecondary())
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
                EmptyState(
                    icon = { Icon(Icons.Rounded.SearchOff, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                    title = "未找到对应物品",
                    subtitle = "换个关键词试试"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
