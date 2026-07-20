package com.nanji.lootarchive.ui.additem

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.GlassCard
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

    // 编辑模式初始化
    LaunchedEffect(editItemId) {
        viewModel.initEditMode(editItemId)
    }

    // 保存成功后返回
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "编辑物品" else "新增物品")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveItem() },
                        enabled = !uiState.isLoading
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                Text("所属分类 *", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.categories.isEmpty()) {
                    Text("暂无分类，请先在分类管理中创建", style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.categories.forEach { category ->
                            FilterChip(
                                selected = uiState.categoryId == category.id,
                                onClick = { viewModel.updateCategoryId(category.id) },
                                label = { Text(category.name) }
                            )
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
                                    val cal = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }
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
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                Text("保修信息", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.warrantyPeriodDays,
                        onValueChange = viewModel::updateWarrantyPeriodDays,
                        label = { Text("保修天数") },
                        placeholder = { Text("365") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    // 保修到期日
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                uiState.warrantyExpiryDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val cal = Calendar.getInstance().apply {
                                            set(year, month, day)
                                        }
                                        viewModel.updateWarrantyExpiryDate(cal.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            uiState.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "到期日",
                            style = MaterialTheme.typography.bodySmall
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

            // 照片区域
            GlassCard {
                Text("物品照片", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 已选照片预览占位
                    if (uiState.photoUris.isEmpty()) {
                        Text(
                            "点击下方按钮添加照片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.photoUris.forEach { uri ->
                            // TODO: 实际使用 Coil 加载照片缩略图
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Image, contentDescription = null)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* TODO: 启动照片选择器 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("拍摄/选择照片")
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

            // 底部留白
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
