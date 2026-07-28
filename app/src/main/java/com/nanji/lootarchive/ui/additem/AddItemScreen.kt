package com.nanji.lootarchive.ui.additem

import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.WheelDatePickerDialog
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.PhotoUtil
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun AddItemScreen(
    editItemId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit = {},
    photoSession: Int = 0,
    viewModel: AddItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Material3 日期选择器状态
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showWarrantyDatePicker by remember { mutableStateOf(false) }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            PhotoUtil.savePhotoFromUri(context, uri)?.let { path ->
                viewModel.addPhotoPath(path)
            }
        }
    }

    // 编辑模式初始化（只在首次进入时触发，避免从相机返回时重复 resetForm）
    var didInit by remember { mutableStateOf(false) }
    LaunchedEffect(editItemId) {
        if (!didInit) {
            viewModel.initEditMode(editItemId)
            didInit = true
        }
    }

    // 从 CameraScreen 返回的照片路径
    LaunchedEffect(photoSession) {
        if (photoSession > 0) {
            val paths = com.nanji.lootarchive.util.PhotoQueue.consume()
            paths.forEach { path -> viewModel.addPhotoPath(path) }
        }
    }

    // 保存成功后返回
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            // 顶部操作栏固定不随内容滚动
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onScreenDisposed(); onNavigateBack() }) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary())
                }
                Text(
                    if (uiState.isEditMode) "编辑物品" else "新增物品",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.saveItem() }, enabled = !uiState.isLoading) {
                    Text("保存", fontWeight = FontWeight.Bold, color = Primary())
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 物品名称
            GlassCard {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("物品名称 *") },
                    placeholder = { Text("如: MacBook Pro 2024") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 分类选择
            GlassCard {
                Text("所属分类 *", style = MaterialTheme.typography.labelLarge, color = TextPrimary())
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.categories.isEmpty()) {
                    Text("暂无分类", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary())
                } else {
                    Column {
                        // 分行展示 FilterChip
                        val chunked = uiState.categories.chunked(3)
                        chunked.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { category ->
                                    FilterChip(
                                        selected = uiState.categoryId == category.id,
                                        onClick = { viewModel.updateCategoryId(category.id) },
                                        label = { Text(category.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary().copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // 购入价格
            GlassCard {
                OutlinedTextField(
                    value = uiState.purchasePrice,
                    onValueChange = viewModel::updatePurchasePrice,
                    label = { Text("购入价格 *") },
                    placeholder = { Text("0.00") },
                    isError = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("¥") }
                )
            }

            // 存放位置
            GlassCard {
                OutlinedTextField(
                    value = uiState.storageLocation,
                    onValueChange = viewModel::updateStorageLocation,
                    label = { Text("存放位置") },
                    placeholder = { Text("如: 卧室书桌抽屉") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) }
                )
            }

            // 购入日期
            GlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPurchaseDatePicker = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Primary())
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("购入日期", style = MaterialTheme.typography.labelSmall, color = TextSecondary())
                        Text(
                            text = uiState.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "点击选择",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary()
                        )
                    }
                }
            }

            // 保修信息
            GlassCard {
                Text("保修信息", fontSize = 16.sp, color = TextPrimary())
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 左：保修天数
                    Column(Modifier.weight(1f)) {
                        Text("保修天数", fontSize = 13.sp, color = TextAuxiliary())
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.warrantyPeriodDays,
                            onValueChange = viewModel::updateWarrantyPeriodDays,
                            placeholder = { Text("365") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    // 右：到期日期
                    Column(Modifier.weight(1f)) {
                        Text("到期日期", fontSize = 13.sp, color = TextAuxiliary())
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("自动计算") },
                            modifier = Modifier.fillMaxWidth().clickable { showWarrantyDatePicker = true }
                        )
                    }
                }
            }

            // 物品描述
            GlassCard {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("物品描述") },
                    placeholder = { Text("如: 配置、成色、入手渠道等") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }

            // ─── 照片区域（完整实现） ───
            GlassCard {
                Text("物品照片", style = MaterialTheme.typography.labelLarge, color = TextPrimary())
                Spacer(modifier = Modifier.height(8.dp))

                // 已选照片预览
                if (uiState.photoPaths.isEmpty()) {
                    Text(
                        "点击下方按钮添加照片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.photoPaths.take(4).forEach { path ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                // 删除按钮（半透明小叉号）
                                IconButton(
                                    onClick = { viewModel.removePhotoPath(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        if (uiState.photoPaths.size > 4) {
                            Text(
                                "+${uiState.photoPaths.size - 4}",
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 拍照 + 相册选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拍照")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("从相册选择")
                    }
                }
            }

            // 错误提示
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        uiState.errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Loading 指示器
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ─── 购入日期选择器（自定义滚轮） ───
    if (showPurchaseDatePicker) {
        WheelDatePickerDialog(
            title = "选择购入日期",
            initialDateMillis = uiState.purchaseDate ?: System.currentTimeMillis(),
            maxDateMillis = System.currentTimeMillis(), // 不能超过今天
            onDismiss = { showPurchaseDatePicker = false },
            onConfirm = { millis ->
                viewModel.updatePurchaseDate(millis)
                showPurchaseDatePicker = false
            }
        )
    }

    // ─── 保修到期日期选择器（自定义滚轮） ───
    if (showWarrantyDatePicker) {
        WheelDatePickerDialog(
            title = "选择保修到期日",
            initialDateMillis = uiState.warrantyExpiryDate ?: System.currentTimeMillis(),
            onDismiss = { showWarrantyDatePicker = false },
            onConfirm = { millis ->
                viewModel.updateWarrantyExpiryDate(millis)
                showWarrantyDatePicker = false
            }
        )
    }
}
