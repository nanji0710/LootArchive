package com.nanji.lootarchive.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Long, onNavigateBack: () -> Unit, onNavigateToEdit: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    if (uiState.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary()) }; return }
    val data = uiState.itemWithPhotos ?: return
    val currencySymbol = remember(uiState.currency) { getCurrencySymbol(uiState.currency) }
    val sheetState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Spacer(Modifier.height(8.dp))
                Text(data.item.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (data.category != null) { Surface(shape = RoundedCornerShape(10.dp), color = Primary().copy(alpha = 0.12f)) { Text(data.category!!.name, color = Primary(), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) } }
                    if (data.item.storageLocation.isNotEmpty()) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(14.dp), tint = TextAuxiliary()); Spacer(Modifier.width(2.dp)); Text(data.item.storageLocation, fontSize = 13.sp, color = TextSecondary()) } }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = TextAuxiliary().copy(alpha = 0.12f))
                DetailRow("购入价格", "${currencySymbol}${numberFormat.format(data.item.purchasePrice)}", valueColor = Primary())
                DetailRow("购入日期", data.item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "未设置")
                val warrantyText = when { data.isWarrantyExpired -> "已过期"; data.isWarrantyExpiring -> "即将到期"; data.item.warrantyExpiryDate != null -> "保修中 · ${dateFormat.format(Date(data.item.warrantyExpiryDate!!))}"; else -> "无保修" }
                val warrantyColor = when { data.isWarrantyExpired -> WarrantyExpired; data.isWarrantyExpiring -> WarrantyExpiring; else -> WarrantyActive }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("保修状态", fontSize = 14.sp, color = TextSecondary(), modifier = Modifier.width(72.dp))
                    Surface(color = warrantyColor.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) { Text(warrantyText, color = warrantyColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) }
                }
                if (data.item.warrantyExpiryDate != null && !data.isWarrantyExpired && data.item.purchaseDate != null) {
                    val td = (data.item.warrantyExpiryDate - data.item.purchaseDate) / (24 * 60 * 60 * 1000)
                    val ed = (System.currentTimeMillis() - data.item.purchaseDate) / (24 * 60 * 60 * 1000)
                    val prog = (ed.toFloat() / td.toFloat()).coerceIn(0f, 1f)
                    val rd = (data.item.warrantyExpiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 72.dp), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { prog }, modifier = Modifier.weight(1f).height(6.dp), color = if (rd <= 7) WarrantyExpiring else WarrantyActive, trackColor = TextAuxiliary().copy(alpha = 0.12f))
                        Spacer(Modifier.width(8.dp)); Text("${rd}天", fontSize = 11.sp, color = warrantyColor, fontWeight = FontWeight.Medium)
                    }
                }
                DetailRow("存放位置", data.item.storageLocation.ifEmpty { "未设置" })
                if (data.item.description.isNotEmpty()) DetailRow("物品描述", data.item.description)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TextAuxiliary().copy(alpha = 0.12f))
                DetailRow("创建时间", dateFormat.format(Date(data.item.createdAt)), valueColor = TextAuxiliary())
                DetailRow("最后修改", dateFormat.format(Date(data.item.updatedAt)), valueColor = TextAuxiliary())
            }
        },
        sheetPeekHeight = 260.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = if (LocalDarkTheme.current) _CardDark else _CardLight,
        sheetTonalElevation = 4.dp,
        containerColor = Color.Transparent,
        topBar = {}
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val screenWidth = maxWidth; val density = LocalDensity.current; val spx = with(density) { screenWidth.toPx() }
            if (data.photos.isNotEmpty()) {
                val sState = rememberScrollState(); val cp = if (spx > 0) (sState.value / spx).roundToInt().coerceIn(0, data.photos.size - 1) else 0
                Row(Modifier.fillMaxSize().horizontalScroll(sState)) { data.photos.forEach { photo -> AsyncImage(model = File(photo.photoPath), contentDescription = null, modifier = Modifier.width(screenWidth).fillMaxHeight(), contentScale = ContentScale.Crop) } }
                Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)))))
                if (data.photos.size > 1) { Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { data.photos.indices.forEach { i -> Surface(Modifier.size(if (i == cp) 8.dp else 6.dp), RoundedCornerShape(4.dp), color = if (i == cp) Color.White else Color.White.copy(alpha = 0.45f)) {} } } }
            } else { Box(Modifier.fillMaxSize().background(Brush.linearGradient(if (LocalDarkTheme.current) listOf(Color(0xFF3D2A1A), Color(0xFF2D2010)) else listOf(Color(0xFFFFD4B8), Color(0xFFFFB890)))), contentAlignment = Alignment.Center) { Text(data.item.name.take(1), fontSize = 90.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.30f), fontFamily = FredokaFont) } }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onNavigateBack, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.22f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.weight(1f))
                Surface(onClick = { onNavigateToEdit(data.item.id) }, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.22f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Edit, "编辑", tint = Color.White, modifier = Modifier.size(18.dp)) } }
                Spacer(Modifier.width(8.dp))
                Surface(onClick = { viewModel.showDeleteConfirm() }, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.22f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(18.dp)) } }
            }
        }
    }
    if (uiState.showDeleteDialog) GlassAlertDialog(title = "删除物品", message = "物品将被移入回收站，可在回收站中恢复。确定删除吗？", confirmText = "删除", dismissText = "取消", onConfirm = { viewModel.deleteItem() }, onDismiss = { viewModel.dismissDeleteDialog() })
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary()) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontSize = 14.sp, color = TextSecondary(), modifier = Modifier.width(72.dp)); Text(value, fontSize = 14.sp, color = valueColor, modifier = Modifier.weight(1f)) }
}
private fun getCurrencySymbol(code: String): String = when (code) { "USD" -> "$"; "EUR" -> "€"; "JPY" -> "¥"; "GBP" -> "£"; else -> "¥" }
