package com.nanji.lootarchive.ui.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.WheelDatePickerDialog
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.PhotoUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showSaleDatePicker by remember { mutableStateOf(false) }
    var showWarrantyDatePicker by remember { mutableStateOf(false) }
    // v5.0: Step wizard state
    var currentStep by remember { mutableIntStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            PhotoUtil.savePhotoFromUri(context, uri)?.let { path -> viewModel.addPhotoPath(path) }
        }
    }

    var didInit by remember { mutableStateOf(false) }
    LaunchedEffect(editItemId) {
        if (!didInit) { viewModel.initEditMode(editItemId); didInit = true }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

    LaunchedEffect(photoSession) {
        if (photoSession > 0) {
            val paths = com.nanji.lootarchive.util.PhotoQueue.consume()
            paths.forEach { path -> viewModel.addPhotoPath(path) }
        }
    }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onScreenDisposed(); onNavigateBack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary())
                }
                Text(
                    if (uiState.isEditMode) "编辑物品" else "新增物品",
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(),
                    modifier = Modifier.weight(1f), fontFamily = FredokaFont
                )
                TextButton(onClick = { viewModel.saveItem() }, enabled = !uiState.isLoading) {
                    Text("保存", fontWeight = FontWeight.SemiBold, color = Primary(), fontSize = 16.sp)
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // v5.0: Step Indicator
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    Surface(
                        Modifier.size(if (i == currentStep) 12.dp else 10.dp),
                        RoundedCornerShape(6.dp),
                        color = when {
                            i < currentStep -> Color(0xFF10B981)
                            i == currentStep -> Primary()
                            else -> TextAuxiliary().copy(alpha = 0.25f)
                        }
                    ) {}
                    if (i < 2) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            Modifier.width(28.dp).height(2.dp),
                            RoundedCornerShape(1.dp),
                            color = if (i < currentStep) Color(0xFF10B981) else TextAuxiliary().copy(alpha = 0.15f)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    when (currentStep) {
                        0 -> "照片"
                        1 -> "📝 详情"
                        else -> "📍 位置与保修"
                    },
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Primary()
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Step 0: 照片 ──
            if (currentStep == 0) {
                ClayCard {
                    Text("物品照片", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(12.dp))

                    if (uiState.photoPaths.isEmpty()) {
                        Text("点击下方按钮添加照片", fontSize = 13.sp, color = TextAuxiliary(), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.photoPaths.take(4).forEach { path ->
                                Box(modifier = Modifier.size(90.dp)) {
                                    AsyncImage(
                                        model = File(path), contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(bottomStart = 10.dp))
                                            .clickable { viewModel.removePhotoPath(path) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Close, "删除", modifier = Modifier.size(14.dp), tint = Color.White)
                                    }
                                }
                            }
                            if (uiState.photoPaths.size > 4) {
                                Text(
                                    "+${uiState.photoPaths.size - 4}",
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp),
                                    fontSize = 14.sp, color = TextAuxiliary()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNavigateToCamera, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary()),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("拍照", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("从相册选择", fontSize = 13.sp)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { currentStep = 1 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary())
                    ) {
                        Text("下一步 →", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Step 1: 详情 ──
            if (currentStep == 1) {
                // 物品名称
                ClayCard {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("物品名称 *") },
                        placeholder = { Text("如: MacBook Pro 2024") },
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // 分类选择
                ClayCard {
                    Text("所属分类 *", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    if (uiState.categories.isEmpty()) {
                        Text("暂无分类", fontSize = 14.sp, color = TextAuxiliary())
                    } else {
                        Column {
                            uiState.categories.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { category ->
                                        FilterChip(
                                            selected = uiState.categoryId == category.id,
                                            onClick = { viewModel.updateCategoryId(category.id) },
                                            label = { Text(category.name, fontSize = 13.sp) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Primary().copy(alpha = 0.15f),
                                                selectedLabelColor = Primary()
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // v5.2 物品状态选择器
                ClayCard {
                    Text("物品状态", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("active" to "在用", "idle" to "闲置", "sold" to "已出", "repair" to "待修", "lost" to "丢失").forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.status == key,
                                onClick = { viewModel.updateStatus(key) },
                                label = { Text(label, fontSize = 13.sp, maxLines = 1) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusColor(key).copy(alpha = 0.15f),
                                    selectedLabelColor = statusColor(key)
                                )
                            )
                        }
                    }
                }

                // v6.6 售出收益（仅已出状态显示）
                AnimatedVisibility(visible = uiState.status == "sold") {
                    ClayCard {
                        Text("售出收益", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.salePriceText,
                            onValueChange = viewModel::updateSalePrice,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("卖出价格（选填）", fontSize = 14.sp, color = TextAuxiliary()) },
                            prefix = { Text("¥", fontSize = 15.sp, color = TextPrimary(), fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary().copy(alpha = 0.5f),
                                unfocusedBorderColor = TextAuxiliary().copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (LocalDarkTheme.current) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f))
                                .clickable { showSaleDatePicker = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, Modifier.size(18.dp), tint = TextAuxiliary())
                            Spacer(Modifier.width(10.dp))
                            Text(
                                uiState.saleDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "售出日期（选填）",
                                fontSize = 14.sp,
                                color = if (uiState.saleDate != null) TextPrimary() else TextAuxiliary(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // v5.2 标签输入
                ClayCard {
                    Text("标签", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    val existingTags = uiState.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (existingTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingTags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { viewModel.removeTag(tag) },
                                    label = { Text(tag, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(Icons.Rounded.Close, "移除$tag", Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.tagInput,
                            onValueChange = viewModel::updateTagInput,
                            placeholder = { Text("输入标签，如 蓝牙、EDC") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.addTag(uiState.tagInput) },
                            enabled = uiState.tagInput.isNotBlank()
                        ) {
                            Icon(Icons.Rounded.AddCircle, "添加标签", tint = Primary())
                        }
                    }
                }

                // 购入价格
                ClayCard {
                    OutlinedTextField(
                        value = uiState.purchasePrice,
                        onValueChange = viewModel::updatePurchasePrice,
                        label = { Text("购入价格 *") },
                        placeholder = { Text("0.00") },
                        isError = uiState.priceError != null,
                        supportingText = uiState.priceError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Text("¥", color = Primary(), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    )
                }

                // 物品描述
                ClayCard {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("物品描述") },
                        placeholder = { Text("如: 配置、成色、入手渠道等") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { currentStep = 0 },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("← 上一步")
                    }
                    Button(
                        onClick = { currentStep = 2 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary())
                    ) {
                        Text("下一步 →", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Step 2: 位置与保修 ──
            if (currentStep == 2) {
                ClayCard {
                    OutlinedTextField(
                        value = uiState.storageLocation,
                        onValueChange = viewModel::updateStorageLocation,
                        label = { Text("存放位置") },
                        placeholder = { Text("如: 卧室书桌抽屉") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = Primary()) }
                    )
                }

                ClayCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showPurchaseDatePicker = true }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = Primary(), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("购入日期", fontSize = 13.sp, color = TextSecondary())
                            Text(
                                text = uiState.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "点击选择",
                                fontSize = 16.sp, color = TextPrimary()
                            )
                        }
                    }
                }

                ClayCard {
                    Text("保修信息", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("保修天数", fontSize = 13.sp, color = TextAuxiliary())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.warrantyPeriodDays,
                                onValueChange = viewModel::updateWarrantyPeriodDays,
                                placeholder = { Text("365") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("到期日期", fontSize = 13.sp, color = TextAuxiliary())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("自动计算") },
                                modifier = Modifier.fillMaxWidth().clickable { showWarrantyDatePicker = true },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("← 上一步")
                    }
                    Button(
                        onClick = { viewModel.saveItem() },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary())
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("完成保存 ✓", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 错误提示
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(uiState.errorMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (uiState.isLoading && uiState.photoPaths.isNotEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = Primary())
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showPurchaseDatePicker) {
        WheelDatePickerDialog(
            title = "选择购入日期",
            initialDateMillis = uiState.purchaseDate ?: System.currentTimeMillis(),
            maxDateMillis = System.currentTimeMillis(),
            onDismiss = { showPurchaseDatePicker = false },
            onConfirm = { millis -> viewModel.updatePurchaseDate(millis); showPurchaseDatePicker = false }
        )
    }
    if (showWarrantyDatePicker) {
        WheelDatePickerDialog(
            title = "选择保修到期日",
            initialDateMillis = uiState.warrantyExpiryDate ?: System.currentTimeMillis(),
            onDismiss = { showWarrantyDatePicker = false },
            onConfirm = { millis -> viewModel.updateWarrantyExpiryDate(millis); showWarrantyDatePicker = false }
        )
    }
    if (showSaleDatePicker) {
        WheelDatePickerDialog(
            title = "选择售出日期",
            initialDateMillis = uiState.saleDate ?: System.currentTimeMillis(),
            maxDateMillis = System.currentTimeMillis(),
            onDismiss = { showSaleDatePicker = false },
            onConfirm = { millis -> viewModel.updateSaleDate(millis); showSaleDatePicker = false }
        )
    }
}
