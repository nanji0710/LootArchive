package com.nanji.lootarchive.ui.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.nanji.lootarchive.util.LocationSuggestions
import com.nanji.lootarchive.util.PhotoUtil
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 新增/编辑物品 —— v6.12 单屏快记。
 *
 * 设计要点（对应 docs/additem-flow-simplification.md）：
 * - 只有「名称」必填；价格必填（价值统计的基准），其余全部可选。
 * - 一屏完成：照片条 → 名称 → 价格 → （可选）更多信息折叠区 → 保存。
 * - 保存后留在页面，露出动作条：再记一件 / 拍照补图 / 相册补图 / 撤销（软删除，可还原）。
 * - 编辑模式沿用同一屏，「更多信息」默认展开，保存后直接返回。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val keyboard = LocalSoftwareKeyboardController.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayText = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val nameFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showWarrantyDatePicker by remember { mutableStateOf(false) }
    var showCustomWarranty by remember { mutableStateOf(false) }

    // 表单会话入口：新增页进入、编辑页进入都从这里开始（替代旧的 resetForm 时序）
    LaunchedEffect(editItemId) { viewModel.startSession(editItemId) }

    // 拍照回传：未保存 → 进表单照片条；已保存 → 直接补到刚保存的物品
    LaunchedEffect(photoSession) {
        if (photoSession > 0) {
            val paths = com.nanji.lootarchive.util.PhotoQueue.consume()
            if (paths.isNotEmpty()) viewModel.onPhotosPicked(paths)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val paths = uris.mapNotNull { PhotoUtil.savePhotoFromUri(context, it) }
        if (paths.isNotEmpty()) viewModel.onPhotosPicked(paths)
    }

    // 编辑模式字段多，默认展开「更多信息」
    LaunchedEffect(uiState.isEditMode) { if (uiState.isEditMode) viewModel.setMoreExpanded(true) }

    // 新增：自动聚焦名称（编辑模式不抢焦点，避免键盘盖住回填内容）
    LaunchedEffect(uiState.isEditMode, uiState.savedItemId) {
        if (!uiState.isEditMode && uiState.savedItemId == null) {
            delay(180)
            runCatching { nameFocus.requestFocus() }
        }
    }

    // 编辑保存后返回；新增保存后留在页面显示动作条
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved && uiState.isEditMode) onNavigateBack() }

    // 轻提示自动消失
    LaunchedEffect(uiState.infoMessage) {
        if (uiState.infoMessage != null) {
            delay(2600)
            viewModel.clearInfoMessage()
        }
    }

    val rowBrush = backgroundBrush()
    val topRowBackdrop = rememberLayerBackdrop {
        drawRect(brush = rowBrush)
        drawContent()
    }
    val actionBarBackdrop = rememberLayerBackdrop {
        drawRect(brush = rowBrush)
        drawContent()
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().layerBackdrop(topRowBackdrop),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary())
                    }
                    Text(
                        if (uiState.isEditMode) "编辑物品" else "新增物品",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary(), modifier = Modifier.weight(1f), fontFamily = FredokaFont
                    )
                    Spacer(Modifier.width(90.dp))
                }
                LiquidGlassButton(
                    onClick = {
                        keyboard?.hide()
                        viewModel.saveItem()
                    },
                    backdrop = topRowBackdrop,
                    enabled = !uiState.isLoading,
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Primary(), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
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
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = AppShape.panel
                ) {
                    Text(uiState.errorMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (uiState.infoMessage != null) {
                Surface(color = Primary().copy(alpha = 0.12f), shape = AppShape.panel) {
                    Text(
                        uiState.infoMessage!!,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall, color = Primary(), fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── 照片条（选填，可后补）──
            ClayCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("照片", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    LiquidGlassButton(
                        onClick = onNavigateToCamera,
                        backdrop = topRowBackdrop,
                        height = 34.dp,
                        horizontalPadding = 12.dp
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, Modifier.size(15.dp), tint = Primary())
                        Spacer(Modifier.width(4.dp))
                        Text("拍照", color = Primary(), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    LiquidGlassButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        backdrop = topRowBackdrop,
                        height = 34.dp,
                        horizontalPadding = 12.dp
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(15.dp), tint = TextPrimary())
                        Spacer(Modifier.width(4.dp))
                        Text("相册", color = TextPrimary(), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (uiState.photoPaths.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.photoPaths.forEach { path ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = File(path), contentDescription = "物品照片",
                                    modifier = Modifier.fillMaxSize().clip(AppShape.panel),
                                    contentScale = ContentScale.Crop
                                )
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
                    }
                }
            }

            // ── 快记：名称（必填）+ 价格（必填）──
            ClayCard {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("物品名称 *") },
                    placeholder = { Text("如: MacBook Pro 2024") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                    singleLine = true,
                    shape = AppShape.panel,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { priceFocus.requestFocus() } })
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.purchasePrice,
                    onValueChange = viewModel::updatePurchasePrice,
                    label = { Text("购入价格 *") },
                    placeholder = { Text("0.00") },
                    isError = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().focusRequester(priceFocus),
                    singleLine = true,
                    shape = AppShape.panel,
                    leadingIcon = { Text("¥", color = Primary(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        viewModel.saveItem()
                    })
                )
            }

            // ── 更多信息（选填，默认收起）──
            ClayCard {
                Row(
                    Modifier.fillMaxWidth().clickable { viewModel.setMoreExpanded(!uiState.moreExpanded) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp), tint = Primary())
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (uiState.moreExpanded) "收起更多信息" else "更多信息（选填）",
                            style = MaterialTheme.typography.bodyMedium, color = TextPrimary(), fontWeight = FontWeight.Medium
                        )
                        if (!uiState.moreExpanded) {
                            Text(
                                "分类 · 状态 · 位置 · 日期 · 保修 · 描述",
                                style = MaterialTheme.typography.labelSmall, color = TextAuxiliary()
                            )
                        }
                    }
                    Icon(
                        if (uiState.moreExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null, Modifier.size(20.dp), tint = TextAuxiliary()
                    )
                }
            }

            AnimatedVisibility(visible = uiState.moreExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 分类
                    ClayCard {
                        Text("所属分类", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        if (uiState.categories.isEmpty()) {
                            Text("暂无分类", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary())
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.categories.forEach { category ->
                                    FilterChip(
                                        selected = uiState.categoryId == category.id,
                                        onClick = { viewModel.updateCategoryId(category.id) },
                                        label = { Text(category.name, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                        shape = AppShape.thumb,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary().copy(alpha = 0.15f),
                                            selectedLabelColor = Primary()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 状态
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

                    // 存放位置（自由输入 + 最近使用/内置建议联想）
                    ClayCard {
                        OutlinedTextField(
                            value = uiState.storageLocation,
                            onValueChange = viewModel::updateStorageLocation,
                            label = { Text("存放位置") },
                            placeholder = { Text("如: 卧室 · 书桌") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = AppShape.panel,
                            leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = Primary()) }
                        )
                        val suggestions = remember(uiState.recentLocations) {
                            (uiState.recentLocations + LocationSuggestions.flatList).distinct().take(12)
                        }
                        if (suggestions.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (uiState.recentLocations.isNotEmpty()) "最近使用 / 常用位置" else "常用位置",
                                style = MaterialTheme.typography.labelSmall, color = TextAuxiliary()
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestions.forEach { location ->
                                    FilterChip(
                                        selected = uiState.storageLocation == location,
                                        onClick = { viewModel.updateStorageLocation(location) },
                                        label = { Text(location, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                        shape = AppShape.thumb,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary().copy(alpha = 0.15f),
                                            selectedLabelColor = Primary()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 购入日期
                    ClayCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = Primary(), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("购入日期", style = MaterialTheme.typography.bodySmall, color = TextSecondary())
                                Text(
                                    uiState.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "点击选择",
                                    style = MaterialTheme.typography.bodyLarge, color = TextPrimary()
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val purchaseText = uiState.purchaseDate?.let { dateFormat.format(Date(it)) }
                            FilterChip(
                                selected = purchaseText == todayText,
                                onClick = viewModel::setPurchaseDateToday,
                                label = { Text("今天", style = MaterialTheme.typography.bodySmall) },
                                shape = AppShape.thumb,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary().copy(alpha = 0.15f),
                                    selectedLabelColor = Primary()
                                )
                            )
                            val yesterdayText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
                            FilterChip(
                                selected = purchaseText == yesterdayText,
                                onClick = viewModel::setPurchaseDateYesterday,
                                label = { Text("昨天", style = MaterialTheme.typography.bodySmall) },
                                shape = AppShape.thumb,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary().copy(alpha = 0.15f),
                                    selectedLabelColor = Primary()
                                )
                            )
                            FilterChip(
                                selected = false,
                                onClick = { showPurchaseDatePicker = true },
                                label = { Text("选择日期", style = MaterialTheme.typography.bodySmall) },
                                shape = AppShape.thumb,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary().copy(alpha = 0.15f),
                                    selectedLabelColor = Primary()
                                )
                            )
                        }
                    }

                    // 保修
                    ClayCard {
                        Text("保修", style = MaterialTheme.typography.bodyMedium, color = TextSecondary(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        val presets = listOf("7 天" to 7, "30 天" to 30, "半年" to 182, "1 年" to 365, "2 年" to 730)
                        val isCustom = showCustomWarranty ||
                            (uiState.warrantyPeriodDays.isNotEmpty() && presets.none { it.second.toString() == uiState.warrantyPeriodDays })
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { (label, days) ->
                                FilterChip(
                                    selected = uiState.warrantyPeriodDays == days.toString(),
                                    onClick = {
                                        showCustomWarranty = false
                                        viewModel.setWarrantyPreset(days)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                    shape = AppShape.thumb,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Primary().copy(alpha = 0.15f),
                                        selectedLabelColor = Primary()
                                    )
                                )
                            }
                            FilterChip(
                                selected = isCustom,
                                onClick = { showCustomWarranty = true },
                                label = { Text("自定义", style = MaterialTheme.typography.bodySmall) },
                                shape = AppShape.thumb,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary().copy(alpha = 0.15f),
                                    selectedLabelColor = Primary()
                                )
                            )
                            if (uiState.warrantyPeriodDays.isNotEmpty() || uiState.warrantyExpiryDate != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showCustomWarranty = false
                                        viewModel.clearWarranty()
                                    },
                                    label = { Text("关闭", style = MaterialTheme.typography.bodySmall) },
                                    shape = AppShape.thumb,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Primary().copy(alpha = 0.15f),
                                        selectedLabelColor = Primary()
                                    )
                                )
                            }
                        }
                        if (isCustom) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.warrantyPeriodDays,
                                onValueChange = viewModel::updateWarrantyPeriodDays,
                                placeholder = { Text("365") },
                                label = { Text("保修天数") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = AppShape.panel,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(AppShape.thumb)
                                .background(if (LocalDarkTheme.current) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f))
                                .clickable { showWarrantyDatePicker = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(18.dp), tint = TextAuxiliary())
                            Spacer(Modifier.width(10.dp))
                            Text(
                                uiState.warrantyExpiryDate?.let { "到期日 ${dateFormat.format(Date(it))}" } ?: "保修到期日（自动计算）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.warrantyExpiryDate != null) TextPrimary() else TextAuxiliary(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 描述
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
                }
            }

            // ── 保存后动作条（仅新增模式）──
            if (uiState.isSaved && !uiState.isEditMode && uiState.savedItemId != null) {
                ClayCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = Color(0xFF10B981))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "已保存「${uiState.savedItemName}」",
                            style = MaterialTheme.typography.bodyMedium, color = TextPrimary(), fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = { viewModel.startNewEntry() },
                            backdrop = actionBarBackdrop,
                            height = 40.dp,
                            horizontalPadding = 14.dp
                        ) {
                            Icon(Icons.Rounded.AddCircleOutline, null, Modifier.size(15.dp), tint = Primary())
                            Spacer(Modifier.width(4.dp))
                            Text("再记一件", color = Primary(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        LiquidGlassButton(
                            onClick = onNavigateToCamera,
                            backdrop = actionBarBackdrop,
                            height = 40.dp,
                            horizontalPadding = 14.dp
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, Modifier.size(15.dp), tint = TextPrimary())
                            Spacer(Modifier.width(4.dp))
                            Text("拍照补图", color = TextPrimary(), style = MaterialTheme.typography.bodySmall)
                        }
                        LiquidGlassButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            backdrop = actionBarBackdrop,
                            height = 40.dp,
                            horizontalPadding = 14.dp
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(15.dp), tint = TextPrimary())
                            Spacer(Modifier.width(4.dp))
                            Text("相册补图", color = TextPrimary(), style = MaterialTheme.typography.bodySmall)
                        }
                        LiquidGlassButton(
                            onClick = { viewModel.undoLastSave() },
                            backdrop = actionBarBackdrop,
                            height = 40.dp,
                            horizontalPadding = 14.dp
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.size(15.dp), tint = Color(0xFFEF4444))
                            Spacer(Modifier.width(4.dp))
                            Text("撤销", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "「再记一件」会沿用分类、位置与购入日期；「撤销」把刚记的这条移入回收站。",
                        style = MaterialTheme.typography.labelSmall, color = TextAuxiliary()
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
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
}
