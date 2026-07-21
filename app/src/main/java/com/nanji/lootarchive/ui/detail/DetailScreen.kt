package com.nanji.lootarchive.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun DetailScreen(
    itemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = uiState.itemWithPhotos ?: return@Scaffold
            val currencySymbol = remember(uiState.currency) { getCurrencySymbol(uiState.currency) }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 内联操作栏（紧凑）
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary(), modifier = Modifier.size(20.dp)) }
                    Text("物品详情", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), modifier = Modifier.weight(1f))
                    IconButton(onClick = { uiState.itemWithPhotos?.let { onNavigateToEdit(it.item.id) } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, "编辑", tint = Primary(), modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(20.dp)) }
                }
                // 照片预览区
                if (data.photos.isNotEmpty()) {
                    GlassCard {
                        // TODO: 使用 HorizontalPager 实现照片左右滑动预览
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.photos.take(3).forEach { photo ->
                                AsyncImage(
                                    model = File(photo.photoPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // 物品名称
                GlassCard {
                    Text(data.item.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary())
                }

                // 分类标签（独立卡片）
                if (data.category != null) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("分类：", fontSize = 14.sp, color = TextSecondary())
                            AssistChip(onClick = {}, label = { Text(data.category!!.name, color = Primary()) })
                        }
                    }
                }

                // 价格与保修
                GlassCard {
                    DetailRow("购入价格", "${currencySymbol}${numberFormat.format(data.item.purchasePrice)}")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("购入日期", data.item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "未设置")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 保修状态
                    val warrantyText = when {
                        data.isWarrantyExpired -> "已过期"
                        data.isWarrantyExpiring -> "即将到期"
                        data.item.warrantyExpiryDate != null -> "保修中 · ${dateFormat.format(Date(data.item.warrantyExpiryDate!!))}"
                        else -> "无保修"
                    }
                    val warrantyColor = when {
                        data.isWarrantyExpired -> WarrantyExpired
                        data.isWarrantyExpiring -> WarrantyExpiring
                        else -> WarrantyActive
                    }
                    DetailRow(
                        "保修状态",
                        warrantyText,
                        valueColor = warrantyColor
                    )
                }

                // 存放与描述
                GlassCard {
                    DetailRow("存放位置", data.item.storageLocation.ifEmpty { "未设置" })
                    if (data.item.description.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("物品描述", data.item.description)
                    }
                }

                // 时间信息
                GlassCard {
                    DetailRow("创建时间", dateFormat.format(Date(data.item.createdAt)))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("最后修改", dateFormat.format(Date(data.item.updatedAt)))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 删除确认对话框
    if (uiState.showDeleteDialog) {
        GlassAlertDialog(
            title = "删除物品",
            message = "物品将被移入回收站，可在设置中彻底清空。确定删除吗？",
            confirmText = "删除",
            dismissText = "取消",
            onConfirm = { viewModel.deleteItem() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun getCurrencySymbol(code: String): String = when (code) {
    "USD" -> "$"
    "EUR" -> "€"
    "JPY" -> "¥"
    "GBP" -> "£"
    else -> "¥"
}
