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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.WheelDatePickerDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
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

    // R2: 向导按钮局部捕获层（每行独立，禁止采样页面层）
    val wizardRowBrush = backgroundBrush()
    val saveRowBackdrop = rememberLayerBackdrop {
        drawRect(brush = wizardRowBrush)
        drawContent()
    }
    val step1RowBackdrop = rememberLayerBackdrop {
        drawRect(brush = wizardRowBrush)
        drawContent()
    }
    val step1NextRowBackdrop = rememberLayerBackdrop {
        drawRect(brush = wizardRowBrush)
        drawContent()
    }
    val step2RowBackdrop = rememberLayerBackdrop {
        drawRect(brush = wizardRowBrush)
        drawContent()
    }
    val step3RowBackdrop = rememberLayerBackdrop {
        drawRect(brush = wizardRowBrush)
        drawContent()
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().layerBackdrop(saveRowBackdrop),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onScreenDisposed(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary())
                    }
                    Text(
                        if (uiState.isEditMode) "编辑物品" else "新增物品",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(),
                        modifier = Modifier.weight(1f), fontFamily = FredokaFont
                    )
                    Spacer(Modifier.width(90.dp))
                }
                LiquidGlassButton(
                    onClick = { viewModel.saveItem() },
                    backdrop = saveRowBackdrop,
                    enabled = !uiState.isLoading,
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("保存", color = Primary(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                        AppShape.chip,
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
                            AppShape.bar,
                            color = if (i < currentStep) Color(0xFF10B981) else TextAuxiliary().copy(alpha = 0.15f)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
            // 错误提示 — 置于表单顶部，任何步骤保存失败都立即可见
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = AppShape.panel
                ) {
                    Text(uiState.errorMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    when (currentStep) {
                        0 -> "照片"
                        1 -> "📝 详情"
                        else -> "📍 位置与保修"
                    },
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    color = Primary()
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Step 0: 照片 ──
            if (currentStep == 0) {
                ClayCard {
                    Text("物品照片", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(12.dp))

                    if (uiState.photoPaths.isEmpty()) {
                        Text("点击下方按钮添加照片", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary(), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.photoPaths.take(4).forEach { path ->
                                Box(modifier = Modifier.size(90.dp)) {
                                    AsyncImage(
                                        model = File(path), contentDescription = "物品照片",
                                        modifier = Modifier.fillMaxSize().clip(AppShape.panel),
                                        contentScale = ContentScale.Crop
                                    )
                                    // 48dp 触摸区包裹 24dp 视觉删除钮（满足最小触摸目标）
                                    Box(
                                        modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Box(
                                            modifier = Modifier.size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(bottomStart = AppRadius.md))
                                                .clickable { viewModel.removePhotoPath(path) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Close, "删除", modifier = Modifier.size(14.dp), tint = Color.White)
                                        }
                                    }
                                }
                            }
                            if (uiState.photoPaths.size > 4) {
                                Text(
                                    "+${uiState.photoPaths.size - 4}",
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().layerBackdrop(step1RowBackdrop),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(Modifier.width(224.dp).height(48.dp))
                        }
                        Row(
                            Modifier.fillMaxWidth().align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = onNavigateToCamera, modifier = Modifier.weight(1f),
                                backdrop = step1RowBackdrop,
                                height = 48.dp,
                                horizontalPadding = 20.dp,
                                tint = Primary()
                            ) {
                                Icon(Icons.Rounded.CameraAlt, null, Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("拍照", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                            LiquidGlassButton(
                                onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f),
                                backdrop = step1RowBackdrop,
                                height = 48.dp,
                                horizontalPadding = 20.dp
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(16.dp), tint = TextPrimary())
                                Spacer(Modifier.width(4.dp))
                                Text("从相册选择", color = TextPrimary(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().layerBackdrop(step1NextRowBackdrop), horizontalArrangement = Arrangement.End) {
                        Spacer(Modifier.width(104.dp).height(48.dp))
                    }
                    LiquidGlassButton(
                        onClick = { currentStep = 1 },
                        backdrop = step1NextRowBackdrop,
                        height = 48.dp,
                        horizontalPadding = 20.dp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("下一步 →", color = Primary(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
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
                        shape = AppShape.panel
                    )
                }

                // 分类选择
                ClayCard {
                    Text("所属分类 *", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    if (uiState.categories.isEmpty()) {
                        Text("暂无分类", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary())
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
                                            label = { Text(category.name, style = MaterialTheme.typography.bodySmall) },
                                            shape = AppShape.thumb,
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
                    Text("物品状态", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ItemStatus.entries.forEach { status ->
                            FilterChip(
                                selected = uiState.status == status.code,
                                onClick = { viewModel.updateStatus(status.code) },
                                label = { Text(status.label, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                shape = AppShape.thumb,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusColor(status.code).copy(alpha = 0.15f),
                                    selectedLabelColor = statusColor(status.code)
                                )
                            )
                        }
                    }
                }

                // v6.6 售出收益（仅已出状态显示）
                AnimatedVisibility(visible = ItemStatus.fromCode(uiState.status) == ItemStatus.SOLD) {
                    ClayCard {
                        Text("售出收益", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.salePriceText,
                            onValueChange = viewModel::updateSalePrice,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("卖出价格（选填）", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary()) },
                            prefix = { Text("¥", style = MaterialTheme.typography.labelLarge, color = TextPrimary(), fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            shape = AppShape.thumb,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary().copy(alpha = 0.5f),
                                unfocusedBorderColor = TextAuxiliary().copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(AppShape.thumb)
                                .background(if (LocalDarkTheme.current) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f))
                                .clickable { showSaleDatePicker = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, Modifier.size(18.dp), tint = TextAuxiliary())
                            Spacer(Modifier.width(10.dp))
                            Text(
                                uiState.saleDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "售出日期（选填）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.saleDate != null) TextPrimary() else TextAuxiliary(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // v5.2 标签输入
                ClayCard {
                    Text("标签", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
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
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        Icon(Icons.Rounded.Close, "移除$tag", Modifier.size(14.dp))
                                    },
                                    shape = AppShape.thumb
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
                            shape = AppShape.panel
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
                        shape = AppShape.panel,
                        leadingIcon = { Text("¥", color = Primary(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) }
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
                        shape = AppShape.panel
                    )
                }

                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().layerBackdrop(step2RowBackdrop), horizontalArrangement = Arrangement.SpaceBetween) {
                        Spacer(Modifier.width(210.dp).height(48.dp))
                    }
                    Row(Modifier.fillMaxWidth().align(Alignment.CenterEnd), horizontalArrangement = Arrangement.SpaceBetween) {
                        LiquidGlassButton(
                            onClick = { currentStep = 0 },
                            backdrop = step2RowBackdrop,
                            height = 48.dp,
                            horizontalPadding = 20.dp
                        ) {
                            Text("← 上一步", color = TextPrimary(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        }
                        LiquidGlassButton(
                            onClick = { currentStep = 2 },
                            backdrop = step2RowBackdrop,
                            height = 48.dp,
                            horizontalPadding = 20.dp
                        ) {
                            Text("下一步 →", color = Primary(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
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
                        shape = AppShape.panel,
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
                            Text("购入日期", style = MaterialTheme.typography.bodySmall, color = TextSecondary())
                            Text(
                                text = uiState.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "点击选择",
                                style = MaterialTheme.typography.bodyLarge, color = TextPrimary()
                            )
                        }
                    }
                }

                ClayCard {
                    Text("保修信息", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("保修天数", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.warrantyPeriodDays,
                                onValueChange = viewModel::updateWarrantyPeriodDays,
                                placeholder = { Text("365") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = AppShape.panel
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("到期日期", style = MaterialTheme.typography.bodySmall, color = TextAuxiliary())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("自动计算") },
                                modifier = Modifier.fillMaxWidth().clickable { showWarrantyDatePicker = true },
                                shape = AppShape.panel
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().layerBackdrop(step3RowBackdrop), horizontalArrangement = Arrangement.SpaceBetween) {
                        Spacer(Modifier.width(216.dp).height(48.dp))
                    }
                    Row(Modifier.fillMaxWidth().align(Alignment.CenterEnd), horizontalArrangement = Arrangement.SpaceBetween) {
                        LiquidGlassButton(
                            onClick = { currentStep = 1 },
                            backdrop = step3RowBackdrop,
                            height = 48.dp,
                            horizontalPadding = 20.dp
                        ) {
                            Text("← 上一步", color = TextPrimary(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        }
                        LiquidGlassButton(
                            onClick = { viewModel.saveItem() },
                            backdrop = step3RowBackdrop,
                            enabled = !uiState.isLoading,
                            height = 48.dp,
                            horizontalPadding = 20.dp
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Primary(), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("完成保存 ✓", color = Primary(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
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
