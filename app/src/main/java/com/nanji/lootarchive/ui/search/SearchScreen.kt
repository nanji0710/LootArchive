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
import com.nanji.lootarchive.domain.model.ItemStatus
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
                    shape = AppShape.sheet,
                    color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.08f) else Color.White,
                    shadowElevation = 2.dp
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = { Text("搜索物品名称 / 存放位置...", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary()) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        shape = AppShape.sheet,
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

            // 结果统计
            if (uiState.query.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("找到 ${uiState.results.size} 件物品", style = MaterialTheme.typography.bodySmall, color = TextPrimary(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                }
            }

            // 搜索结果 / 搜索历史 / 空状态
            if (uiState.results.isEmpty() && uiState.query.isEmpty()) {
                if (uiState.recentSearches.isNotEmpty()) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("最近搜索", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("清空", style = MaterialTheme.typography.bodySmall, color = Primary())
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.recentSearches.take(8).forEach { query ->
                                Surface(
                                    onClick = { viewModel.updateQuery(query); viewModel.submitSearch() },
                                    shape = AppShape.thumb,
                                    color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.History, null, Modifier.size(14.dp), tint = TextAuxiliary())
                                        Spacer(Modifier.width(10.dp))
                                        Text(query, style = MaterialTheme.typography.bodyMedium, color = TextPrimary())
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
                    columns = GridCells.Adaptive(minSize = 160.dp),
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
        Box(Modifier.fillMaxWidth().height(110.dp).clip(AppShape.panel)) {
            if (firstPhotoPath != null) {
                AsyncImage(model = File(firstPhotoPath), contentDescription = null, modifier = Modifier.fillMaxSize().clip(AppShape.panel), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Primary().copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(36.dp), tint = TextAuxiliary().copy(alpha = 0.35f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("¥${numberFormat.format(item.purchasePrice)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary(), fontFamily = FredokaFont)
    }
}
