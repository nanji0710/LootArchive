package com.nanji.lootarchive.ui.additem

import androidx.compose.ui.graphics.Color
import android.app.DatePickerDialog
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nanji.lootarchive.ui.component.GlassCard
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
    viewModel: AddItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 照片选择弹窗控制
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    // 用于相机拍照的临时文件 URI
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

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

    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            // 相机已直接将照片写入我们提供的文件
            val savedPath = PhotoUtil.savePhotoFromUri(context, cameraPhotoUri!!)
            if (savedPath != null) viewModel.addPhotoPath(savedPath)
        }
    }

    // 编辑模式初始化
    LaunchedEffect(editItemId) {
        viewModel.initEditMode(editItemId)
    }

    // 保存成功后返回
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部操作栏：返回 + 标题 + 保存
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
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
                Text("所属分类 *", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.categories.isEmpty()) {
                    Text("暂无分类", style = MaterialTheme.typography.bodySmall)
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
                                        label = { Text(category.name) }
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
                        .clickable {
                            val calendar = Calendar.getInstance()
                            uiState.purchaseDate?.let { calendar.timeInMillis = it }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val cal = Calendar.getInstance().apply { set(year, month, day) }
                                    viewModel.updatePurchaseDate(cal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("购入日期", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = uiState.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "点击选择",
                            style = MaterialTheme.typography.bodyLarge
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
                            modifier = Modifier.fillMaxWidth().clickable {
                                val calendar = Calendar.getInstance()
                                uiState.warrantyExpiryDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val cal = Calendar.getInstance().apply { set(year, month, day) }
                                        viewModel.updateWarrantyExpiryDate(cal.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
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
                Text("物品照片", style = MaterialTheme.typography.labelLarge)
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

                // 拍照 / 选择按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                            photoFile.parentFile?.mkdirs()
                            photoFile.createNewFile()
                            cameraPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                            cameraLauncher.launch(cameraPhotoUri!!)
                        },
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
                        Text("相册")
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
}
