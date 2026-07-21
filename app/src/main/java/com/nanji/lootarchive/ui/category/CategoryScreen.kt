package com.nanji.lootarchive.ui.category

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary()) }
                Text("分类管理", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showAddDialog() }) { Icon(Icons.Filled.Add, "新增", tint = Primary()) }
            }
            Spacer(Modifier.height(12.dp))
        if (uiState.categories.isEmpty() && !uiState.isLoading) {
            EmptyState(
                icon = { Icon(Icons.Filled.Category, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                title = "暂无分类",
                subtitle = "点击右下角按钮新增分类",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryListItem(
                        category = category,
                        itemCount = uiState.categoryItemCounts[category.id] ?: 0,
                        onEdit = { viewModel.showEditDialog(category) },
                        onDelete = { viewModel.showDeleteDialog(category) }
                    )
                }
            }
        }
        } // Column close
    }

    // 新增/编辑对话框
    if (uiState.showAddDialog || uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(if (uiState.showAddDialog) "新增分类" else "编辑分类") },
            text = {
                OutlinedTextField(
                    value = uiState.dialogName,
                    onValueChange = viewModel::updateDialogName,
                    label = { Text("分类名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { if (uiState.showAddDialog) viewModel.addCategory() else viewModel.updateCategory() }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("删除分类") },
            text = { Text("分类删除后，该分类下的物品将归入「其他」分类。确定删除？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryEntity,
    itemCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("$itemCount 件物品", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
