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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
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
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            ) {
                // 操作栏
                Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary(), modifier = Modifier.size(20.dp)) }
                    Text("物品详情", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), modifier = Modifier.weight(1f))
                    IconButton(onClick = { uiState.itemWithPhotos?.let { onNavigateToEdit(it.item.id) } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, "编辑", tint = Primary(), modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(20.dp)) }
                }

                // ── 主图区域 300dp ──
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (data.photos.isNotEmpty()) {
                        // 水平滚动浏览所有照片
                        Row(
                            modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
                        ) {
                            data.photos.forEach { photo ->
                                AsyncImage(
                                    model = File(photo.photoPath),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        // 底部渐变遮罩（过渡到内容卡片）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                    )
                                )
                        )
                        // 照片计数
                        if (data.photos.size > 1) {
                            Text(
                                text = "1/${data.photos.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        // 无照片：暖色渐变 + 首字占位
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFE8A850), Color(0xFFD4863C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = data.item.name.take(1),
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── 内容卡片（向上偏移 20dp 重叠主图） ──
                GlassCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-20).dp)
                ) {
                    // 物品名称
                    Text(
                        data.item.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 分类标签 + 位置
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (data.category != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text(data.category!!.name, color = Primary(), fontSize = 13.sp) }
                            )
                        }
                        Text(
                            data.item.storageLocation.ifEmpty { "" },
                            fontSize = 13.sp,
                            color = TextSecondary()
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = TextAuxiliary().copy(alpha = 0.2f))

                    // 详细字段
                    DetailRow("购入价格", "${currencySymbol}${numberFormat.format(data.item.purchasePrice)}")
                    DetailRow("购入日期", data.item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "未设置")

                    // 保修状态（PillTag 风格）
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("保修状态", fontSize = 14.sp, color = TextSecondary(), modifier = Modifier.width(80.dp))
                        Surface(
                            color = warrantyColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                warrantyText,
                                color = warrantyColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    DetailRow("存放位置", data.item.storageLocation.ifEmpty { "未设置" })
                    if (data.item.description.isNotEmpty()) {
                        DetailRow("物品描述", data.item.description)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = TextAuxiliary().copy(alpha = 0.2f))

                    DetailRow("创建时间", dateFormat.format(Date(data.item.createdAt)))
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
    valueColor: Color = TextPrimary()
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary(),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            modifier = Modifier.weight(1f)
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
