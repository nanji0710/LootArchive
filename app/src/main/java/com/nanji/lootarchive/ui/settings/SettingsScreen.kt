package com.nanji.lootarchive.ui.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.GlassAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isTabMode: Boolean = false,  // true = 嵌入Tab显示，不渲染独立TopBar
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showAppNameDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            if (!isTabMode) {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } }
                )
            }
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
            // 通用设置
            SectionTitle("通用设置")

            GlassCard {
                SettingItem(
                    icon = Icons.Filled.Badge,
                    title = "APP名称",
                    subtitle = uiState.appName,
                    onClick = {
                        editText = uiState.appName
                        showAppNameDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SettingItem(
                    icon = Icons.Filled.AttachMoney,
                    title = "价格单位",
                    subtitle = when (uiState.currency) {
                        "CNY" -> "人民币 (¥)"
                        "USD" -> "美元 ($)"
                        "EUR" -> "欧元 (€)"
                        "JPY" -> "日元 (¥)"
                        else -> uiState.currency
                    },
                    onClick = { showCurrencyDialog = true }
                )
            }

            // 提醒设置
            SectionTitle("提醒设置")

            GlassCard {
                SettingItem(
                    icon = Icons.Filled.Notifications,
                    title = "保修到期提醒",
                    subtitle = "提前 ${uiState.warrantyReminderDays} 天",
                    onClick = {
                        editText = uiState.warrantyReminderDays.toString()
                        showReminderDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Backup, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("定期备份提醒", fontWeight = FontWeight.Medium)
                        Text(
                            if (uiState.backupReminderEnabled) "每月${uiState.backupReminderDay}日提醒" else "已关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.backupReminderEnabled,
                        onCheckedChange = { viewModel.setBackupReminder(it) }
                    )
                }
            }

            // 显示设置
            SectionTitle("显示设置")

            GlassCard {
                // 主题切换改用弹窗三选一，更直观
                var showThemeDialog by remember { mutableStateOf(false) }
                SettingItem(
                    icon = Icons.Filled.Palette,
                    title = "主题模式",
                    subtitle = when (uiState.themeMode) {
                        "system" -> "跟随系统"
                        "light" -> "浅色模式"
                        "dark" -> "深色模式"
                        else -> uiState.themeMode
                    },
                    onClick = { showThemeDialog = true }
                )

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("选择主题模式") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "system" to "跟随系统",
                                    "light" to "浅色模式",
                                    "dark" to "深色模式"
                                ).forEach { (mode, label) ->
                                    Surface(
                                        onClick = {
                                            viewModel.setThemeMode(mode)
                                            showThemeDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (uiState.themeMode == mode)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (uiState.themeMode == mode) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.width(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(label, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
                        }
                    )
                }
            }

            // 数据管理
            SectionTitle("数据管理")

            GlassCard {
                SettingItem(
                    icon = Icons.Filled.DeleteSweep,
                    title = "清空回收站",
                    subtitle = "回收站中有 ${uiState.trashItemCount} 件物品",
                    onClick = {
                        if (uiState.trashItemCount > 0) {
                            showEmptyTrashDialog = true
                        }
                    }
                )
            }

            // 关于
            SectionTitle("关于")
            GlassCard {
                SettingItem(
                    icon = Icons.Filled.Info,
                    title = "拾物集 LootArchive",
                    subtitle = "v1.1.1 · 纯本地资产管理工具",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 货币选择对话框
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("选择货币单位") },
            text = {
                Column {
                    listOf("CNY" to "人民币 (¥)", "USD" to "美元 ($)", "EUR" to "欧元 (€)", "JPY" to "日元 (¥)", "GBP" to "英镑 (£)").forEach { (code, label) ->
                        Surface(
                            onClick = {
                                viewModel.setCurrency(code)
                                showCurrencyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (uiState.currency == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Text(label, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("取消") } }
        )
    }

    // 提醒阈值对话框
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("保修提醒阈值") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("提前天数") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editText.toIntOrNull()?.let { viewModel.setWarrantyReminderDays(it) }
                    showReminderDialog = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("取消") } }
        )
    }

    // 清空回收站确认
    if (showEmptyTrashDialog) {
        GlassAlertDialog(
            title = "清空回收站",
            message = "回收站中的 ${uiState.trashItemCount} 件物品将被永久删除，无法恢复。确定清空？",
            confirmText = "清空",
            dismissText = "取消",
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }

    // APP名称编辑
    if (showAppNameDialog) {
        AlertDialog(
            onDismissRequest = { showAppNameDialog = false },
            title = { Text("自定义APP名称") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("APP名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editText.isNotBlank()) viewModel.setAppName(editText.trim())
                    showAppNameDialog = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showAppNameDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
