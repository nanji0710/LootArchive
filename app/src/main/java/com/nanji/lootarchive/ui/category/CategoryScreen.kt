package com.nanji.lootarchive.ui.category

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.ui.component.ClayCard
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
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.Add, "新增", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary()) }
                Text("分类管理", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
            }
            Spacer(Modifier.height(10.dp))
        if (uiState.categories.isEmpty() && !uiState.isLoading) {
            EmptyState(
                icon = { Icon(Icons.Filled.Category, null, modifier = Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.5f)) },
                title = "暂无分类",
                subtitle = "点击右下角按钮新增分类"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (uiState.showAddDialog) "新增分类" else "编辑分类", color = TextPrimary(), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = uiState.dialogName,
                    onValueChange = viewModel::updateDialogName,
                    label = { Text("分类名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary(),
                        unfocusedTextColor = TextPrimary(),
                        focusedLabelColor = Primary(),
                        unfocusedLabelColor = TextSecondary()
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { if (uiState.showAddDialog) viewModel.addCategory() else viewModel.updateCategory() }) {
                    Text("确认", color = Primary())
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("取消", color = TextSecondary())
                }
            }
        )
    }

    // 删除确认
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除分类", color = TextPrimary(), fontWeight = FontWeight.Bold) },
            text = { Text("分类删除后，该分类下的物品将归入「其他」分类。确定删除？", color = TextSecondary()) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("取消", color = TextSecondary()) }
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
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, null, tint = Primary(), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                Text("$itemCount 件物品", fontSize = 13.sp, color = TextSecondary())
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, "编辑", tint = Primary(), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(18.dp))
            }
        }
    }
}
