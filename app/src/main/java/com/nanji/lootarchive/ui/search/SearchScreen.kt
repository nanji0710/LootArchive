package com.nanji.lootarchive.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = { Text("搜索物品名称 / 存放位置 / 购入备注", fontSize = 14.sp, color = TextAuxiliary) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFEEEEEE), unfocusedContainerColor = Color(0xFFEEEEEE)),
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextAuxiliary) },
                        trailingIcon = { if (uiState.query.isNotEmpty()) IconButton(onClick = { viewModel.updateQuery("") }) { Icon(Icons.Filled.Close, "清除", Modifier.size(20.dp)) } }
                    )
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 筛选标签栏
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val filters = listOf("全部" to null, "物品名称" to "name", "存放位置" to "location", "购入备注" to "desc", "保修信息" to "warranty")
                items(filters.size) { index ->
                    val (label, _) = filters[index]
                    val selected = uiState.activeFilter == filters[index].second
                    TextButton(onClick = { viewModel.setActiveFilter(filters[index].second) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, fontSize = 14.sp, color = if (selected) Primary else TextAuxiliary)
                            if (selected) Spacer(Modifier.width(20.dp).height(2.dp).background(Primary, RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }

            // 结果统计+排序
            if (uiState.query.isNotEmpty() || uiState.activeFilter != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("找到 ${uiState.results.size} 件物品", fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    var showSort by remember { mutableStateOf(false) }
                    Box { TextButton(onClick = { showSort = true }) { Text("排序", fontSize = 14.sp, color = Primary) }
                        DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                            listOf("price_desc" to "价格从高到低", "date_new" to "购入时间最新", "warranty" to "保修到期优先").forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSort(key); showSort = false })
                            }
                        }
                    }
                }
            }

            // 结果列表
            if (uiState.results.isEmpty() && uiState.query.isEmpty() && uiState.activeFilter == null) {
                EmptyState(
                    icon = { Icon(Icons.Filled.Search, null, Modifier.size(100.dp), tint = Color(0xFFBBBBBB)) },
                    title = "搜索物品",
                    subtitle = "输入关键词查找你的物品"
                )
            } else if (uiState.results.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Filled.SearchOff, null, Modifier.size(100.dp), tint = Color(0xFFBBBBBB)) },
                    title = "未找到对应物品",
                    subtitle = "换个关键词试试"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results, key = { it.id }) { item -> SearchItemCard(item, numberFormat) { onNavigateToDetail(item.id) } }
                }
            }
        }
    }
}

@Composable
private fun SearchItemCard(item: ItemEntity, numberFormat: NumberFormat, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Surface(Modifier.fillMaxWidth().height(100.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Image, null, Modifier.size(32.dp), tint = Color(0xFFBBBBBB)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, fontSize = 18.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("¥${numberFormat.format(item.purchasePrice)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
    }
}
