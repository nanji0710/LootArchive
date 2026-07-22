package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.theme.ChartColors

@Composable
fun CategoryDrawer(
    selectedFilter: Pair<Long, String>?,
    onCategorySelected: (Long, String) -> Unit,
    onClose: () -> Unit,
    viewModel: CategoryDrawerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(16.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "物品分类",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { /* TODO: 编辑分类模式 */ }) {
                Text("编辑")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 全部物品（固定首行）
            item {
                DrawerCategoryItem(
                    name = "全部物品",
                    count = uiState.totalItemCount,
                    colorIndex = -1,
                    isSelected = selectedFilter == null,
                    onClick = { onCategorySelected(-1L, "全部物品") }
                )
            }

            // 自定义分类列表
            items(uiState.categories) { category ->
                DrawerCategoryItem(
                    name = category.name,
                    count = uiState.categoryCounts[category.id] ?: 0,
                    colorIndex = uiState.categories.indexOf(category),
                    isSelected = selectedFilter?.first == category.id,
                    onClick = { onCategorySelected(category.id, category.name) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部新建分类
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { viewModel.showAddDialog() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建分类", color = MaterialTheme.colorScheme.primary)
        }

        // 新增分类弹窗
        if (uiState.showAddDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("新建分类") },
                text = {
                    OutlinedTextField(
                        value = uiState.newCategoryName,
                        onValueChange = viewModel::updateNewCategoryName,
                        label = { Text("分类名称") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.addCategory() }) { Text("确认") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    name: String,
    count: Int,
    colorIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (colorIndex >= 0) ChartColors[colorIndex % ChartColors.size] else Color.Gray
    val bgColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色标识条
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(2.dp),
                color = color
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$count",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
