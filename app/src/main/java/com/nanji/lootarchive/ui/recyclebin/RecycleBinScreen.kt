package com.nanji.lootarchive.ui.recyclebin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {}
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = TextPrimary(), modifier = Modifier.size(22.dp))
                }
                Text("回收站", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont)
                if (uiState.deletedItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.showEmptyConfirm() }) {
                        Text("清空", color = WarrantyExpired, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                RoundedCornerShape(14.dp),
                color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
            ) {
                Text(
                    "删除的物品保留 14 天，到期自动清空",
                    fontSize = 12.sp, color = TextAuxiliary(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary())
                }
            } else if (uiState.deletedItems.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(80.dp), tint = TextAuxiliary().copy(alpha = 0.35f)) },
                    title = "回收站为空",
                    subtitle = "删除的物品会出现在这里"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.deletedItems, key = { it.id }) { item ->
                        TrashItemCard(
                            item = item,
                            dateFormat = dateFormat,
                            numberFormat = numberFormat,
                            onRestore = { viewModel.restoreItem(item) },
                            onDelete = { viewModel.showDeleteConfirm(item) }
                        )
                    }
                }
            }

            if (uiState.message != null) {
                Surface(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    RoundedCornerShape(14.dp),
                    color = Primary().copy(alpha = 0.12f)
                ) {
                    Text(
                        uiState.message!!,
                        modifier = Modifier.padding(14.dp),
                        color = Primary(), fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (uiState.showDeleteConfirm && uiState.targetItem != null) {
        GlassAlertDialog(
            title = "彻底删除",
            message = "「${uiState.targetItem!!.name}」将被永久删除，无法恢复。确定吗？",
            confirmText = "彻底删除", dismissText = "取消",
            onConfirm = { viewModel.hardDeleteItem(uiState.targetItem!!) },
            onDismiss = { viewModel.dismissDeleteConfirm() }
        )
    }
    if (uiState.showEmptyConfirm) {
        GlassAlertDialog(
            title = "清空回收站",
            message = "将彻底删除回收站中 ${uiState.deletedItems.size} 件物品，不可恢复。",
            confirmText = "全部清空", dismissText = "取消",
            onConfirm = { viewModel.emptyTrash() },
            onDismiss = { viewModel.dismissEmptyConfirm() }
        )
    }
}

@Composable
private fun TrashItemCard(
    item: ItemEntity,
    dateFormat: SimpleDateFormat,
    numberFormat: NumberFormat,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥${numberFormat.format(item.purchasePrice)}", fontSize = 13.sp, color = Primary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        item.deletedAt?.let { "删除于 ${dateFormat.format(Date(it))}" } ?: "",
                        fontSize = 11.sp, color = TextAuxiliary()
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onRestore,
                    shape = RoundedCornerShape(10.dp),
                    color = Primary().copy(alpha = 0.10f)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Restore, "还原", tint = Primary(), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("还原", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Primary())
                    }
                }
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    color = WarrantyExpired.copy(alpha = 0.10f)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DeleteForever, "删除", tint = WarrantyExpired, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WarrantyExpired)
                    }
                }
            }
        }
    }
}
