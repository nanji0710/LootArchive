package com.nanji.lootarchive.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.domain.model.ItemWithPhotos
import com.nanji.lootarchive.ui.component.ClayCard
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

    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary())
            }
        } else {
            val data = uiState.itemWithPhotos ?: return@Scaffold
            val currencySymbol = remember(uiState.currency) { getCurrencySymbol(uiState.currency) }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            ) {
                // ── 操作栏 ──
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary(), modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "物品详情", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary(), modifier = Modifier.weight(1f),
                        fontFamily = FredokaFont
                    )
                    IconButton(onClick = { onNavigateToEdit(data.item.id) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Edit, "编辑", tint = Primary(), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(22.dp))
                    }
                }

                // ── 主图区域 260dp ──
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    val screenWidth = maxWidth
                    if (data.photos.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
                        ) {
                            data.photos.forEach { photo ->
                                AsyncImage(
                                    model = File(photo.photoPath),
                                    contentDescription = null,
                                    modifier = Modifier.width(screenWidth).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        // 渐变遮罩（过渡到内容卡）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(72.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                    )
                                )
                        )
                        if (data.photos.size > 1) {
                            Surface(
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                                RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    "1/${data.photos.size}",
                                    color = Color.White, fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        // 占位渐变
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFC090), Color(0xFFFF9D60))
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = data.item.name.take(1),
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.35f),
                                fontFamily = FredokaFont
                            )
                        }
                    }
                }

                // ── 内容卡片（向上偏移 24dp 重叠主图） ──
                ClayCard(
                    modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp)
                ) {
                    Text(
                        data.item.name, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, color = TextPrimary(),
                        fontFamily = FredokaFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (data.category != null) {
                            Surface(shape = RoundedCornerShape(10.dp), color = Primary().copy(alpha = 0.10f)) {
                                Text(
                                    data.category!!.name, color = Primary(), fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (data.item.storageLocation.isNotEmpty()) {
                            Text(data.item.storageLocation, fontSize = 13.sp, color = TextSecondary())
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = TextAuxiliary().copy(alpha = 0.15f))

                    DetailRow("购入价格", "${currencySymbol}${numberFormat.format(data.item.purchasePrice)}")
                    DetailRow("购入日期", data.item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "未设置")

                    // 保修 Pill
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
                        Text("保修状态", fontSize = 14.sp, color = TextSecondary(), modifier = Modifier.width(72.dp))
                        Surface(
                            color = warrantyColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                warrantyText, color = warrantyColor, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    DetailRow("存放位置", data.item.storageLocation.ifEmpty { "未设置" })
                    if (data.item.description.isNotEmpty()) {
                        DetailRow("物品描述", data.item.description)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = TextAuxiliary().copy(alpha = 0.15f))

                    DetailRow("创建时间", dateFormat.format(Date(data.item.createdAt)))
                    DetailRow("最后修改", dateFormat.format(Date(data.item.updatedAt)))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (uiState.showDeleteDialog) {
        GlassAlertDialog(
            title = "删除物品",
            message = "物品将被移入回收站，可在设置中彻底清空。确定删除吗？",
            confirmText = "删除", dismissText = "取消",
            onConfirm = { viewModel.deleteItem() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary()) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary(), modifier = Modifier.width(72.dp))
        Text(value, fontSize = 14.sp, color = valueColor, modifier = Modifier.weight(1f))
    }
}

private fun getCurrencySymbol(code: String): String = when (code) {
    "USD" -> "$"; "EUR" -> "€"; "JPY" -> "¥"; "GBP" -> "£"; else -> "¥"
}
