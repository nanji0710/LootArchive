package com.nanji.lootarchive.ui.category

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector

// v5.0: 分类专属图标映射
private val CategoryIcons = mapOf(
    "食品饮料" to Icons.Rounded.Restaurant,
    "药品保健" to Icons.Rounded.MedicalServices,
    "日用百货" to Icons.Rounded.Checkroom,
    "数码电子" to Icons.Rounded.Devices,
    "服饰鞋包" to Icons.Rounded.Checkroom,
    "书籍文具" to Icons.Rounded.MenuBook,
    "工具器材" to Icons.Rounded.Build,
    "藏品摆件" to Icons.Rounded.Diamond,
    "家居家具" to Icons.Rounded.Chair,
    "其他" to Icons.Rounded.MoreHoriz
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
                Icon(Icons.Rounded.Add, "新增", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary())
                }
                Text("分类管理", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
            }
            Spacer(Modifier.height(10.dp))

            if (uiState.categories.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = { Icon(Icons.Outlined.Category, null, modifier = Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                    title = "暂无分类",
                    subtitle = "点击右下角按钮新增分类"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryGridCard(
                            category = category,
                            itemCount = uiState.categoryItemCounts[category.id] ?: 0,
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.showDeleteDialog(category) }
                        )
                    }
                }
            }
        }
    }

    // 新增/编辑对话框
    if (uiState.showAddDialog || uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    if (uiState.showAddDialog) "新增分类" else "编辑分类",
                    color = TextPrimary(), fontWeight = FontWeight.Bold
                )
            },
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
                TextButton(onClick = {
                    if (uiState.showAddDialog) viewModel.addCategory() else viewModel.updateCategory()
                }) { Text("确认", color = Primary(), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("取消", color = TextSecondary()) }
            }
        )
    }

    // 删除确认
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除分类", color = TextPrimary(), fontWeight = FontWeight.Bold) },
            text = { Text("分类删除后，该分类下的物品将归入「其他」分类。确定删除？", color = TextSecondary()) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("取消", color = TextSecondary()) }
            }
        )
    }
}

@Composable
private fun CategoryGridCard(
    category: CategoryEntity,
    itemCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = CategoryIcons[category.name] ?: Icons.Rounded.Folder
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                Modifier.size(48.dp), RoundedCornerShape(16.dp),
                color = Primary().copy(alpha = 0.10f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, category.name, tint = Primary(), modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(category.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text("$itemCount 件物品", fontSize = 12.sp, color = TextAuxiliary())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit, "编辑", tint = Primary(), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
