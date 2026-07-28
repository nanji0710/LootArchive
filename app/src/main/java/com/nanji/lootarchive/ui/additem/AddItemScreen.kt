package com.nanji.lootarchive.ui.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showWarrantyDatePicker by remember { mutableStateOf(false) }

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
                    Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary())
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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

            // 分类选择 — 卡片式
            ClayCard {
                Text("所属分类 *", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(10.dp))
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
                    leadingIcon = { Text("¥", color = Primary(), fontWeight = FontWeight.Bold) }
                )
            }

            // 存放位置
            ClayCard {
                OutlinedTextField(
                    value = uiState.storageLocation,
                    onValueChange = viewModel::updateStorageLocation,
                    label = { Text("存放位置") },
                    placeholder = { Text("如: 卧室书桌抽屉") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Primary()) }
                )
            }

            // 购入日期
            ClayCard {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPurchaseDatePicker = true }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarToday, null, tint = Primary(), modifier = Modifier.size(22.dp))
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

            // 保修信息
            ClayCard {
                Text("保修信息", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
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

            // 照片
            ClayCard {
                Text("物品照片", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.photoPaths.isEmpty()) {
                    Text("点击下方按钮添加照片", fontSize = 13.sp, color = TextAuxiliary(), modifier = Modifier.padding(vertical = 6.dp))
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.photoPaths.take(4).forEach { path ->
                            Box(modifier = Modifier.size(90.dp)) {
                                AsyncImage(
                                    model = File(path), contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(bottomStart = 8.dp))
                                        .clickable { viewModel.removePhotoPath(path) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, "删除", modifier = Modifier.size(14.dp), tint = Color.White)
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onNavigateToCamera, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("拍照")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("从相册选择")
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

            if (uiState.isLoading) {
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
}
