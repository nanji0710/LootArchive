package com.nanji.lootarchive.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isTabMode: Boolean = false,
    onNavigateToCategory: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var editReminderDays by remember { mutableStateOf("") }
    val context = LocalContext.current

    val bgImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setBackgroundUri(uri.toString())
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setAvatarUri(uri.toString())
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── v5.0 个性化 ──
            SectionHeader(Icons.Rounded.Palette, "个性化")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    // 显示模式
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示模式", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("system" to "跟随", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                                FilterChip(
                                    selected = uiState.themeMode == mode,
                                    onClick = { if (uiState.themeMode != mode) viewModel.setThemeMode(mode) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Primary().copy(alpha = 0.15f),
                                        selectedLabelColor = Primary()
                                    )
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                    // 自定义头像
                    Row(
                        Modifier.fillMaxWidth().clickable { avatarPicker.launch("image/*") }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自定义头像", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        if (uiState.avatarUri.isNotEmpty()) {
                            Surface(
                                onClick = { viewModel.setAvatarUri("") },
                                shape = RoundedCornerShape(8.dp),
                                color = Primary().copy(alpha = 0.10f)
                            ) {
                                Text("还原", fontSize = 12.sp, color = Primary(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                    // v6.3 新手引导
                    val ctx = LocalContext.current
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            viewModel.resetOnboarding()
                            Toast.makeText(ctx, "下次启动时将重新显示引导", Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("重新查看引导", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── v5.0 提醒 ──
            SectionHeader(Icons.Rounded.Notifications, "提醒")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("保修到期提醒", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Primary().copy(alpha = 0.10f)
                            ) {
                                Text(
                                    "提前 ${uiState.warrantyReminderDays} 天",
                                    fontSize = 13.sp, color = Primary(), fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = {
                                editReminderDays = uiState.warrantyReminderDays.toString()
                                showReminderDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp), tint = Primary())
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                    // v6.0 备份提醒开关
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("备份提醒", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        Switch(
                            checked = uiState.backupReminderEnabled,
                            onCheckedChange = { viewModel.setBackupReminder(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Primary().copy(alpha = 0.5f), checkedThumbColor = Primary())
                        )
                    }
                }
            }

            // ── v5.0 存储 ──
            SectionHeader(Icons.Rounded.Storage, "存储")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("缓存大小", fontSize = 15.sp, color = TextPrimary())
                        Spacer(Modifier.height(2.dp))
                        if (uiState.isCalculatingCache) {
                            Text("计算中...", fontSize = 13.sp, color = TextAuxiliary())
                        } else {
                            Text(uiState.cacheSizeFormatted, fontSize = 13.sp, color = TextAuxiliary())
                        }
                    }
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        enabled = !uiState.isClearing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary())
                        } else {
                            Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("清除缓存", fontSize = 13.sp)
                    }
                }
            }

            // ── v5.0 关于 ──
            SectionHeader(Icons.Rounded.Info, "关于")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("拾物集 ItemGlow", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(4.dp))
                    Text("当前版本 v${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = TextAuxiliary())
                    Spacer(Modifier.height(8.dp))
                    Text("数据看板", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(2.dp))
                    Text("回收站: ${uiState.trashItemCount} 件", fontSize = 12.sp, color = TextAuxiliary())
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 弹窗（保持原有逻辑）
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("保修提醒阈值", color = TextPrimary(), fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = editReminderDays,
                    onValueChange = { editReminderDays = it },
                    label = { Text("提前天数") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editReminderDays.toIntOrNull()?.let { viewModel.setWarrantyReminderDays(it) }
                    showReminderDialog = false
                }) { Text("确认", color = Primary()) }
            },
            dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("取消") } }
        )
    }

    if (showClearCacheDialog) {
        GlassAlertDialog(
            title = "清除缓存",
            message = "将清除图片缓存等临时数据（约 ${uiState.cacheSizeFormatted}），不会影响你的物品数据和设置。",
            confirmText = "清除", dismissText = "取消",
            onConfirm = { viewModel.clearCache(); showClearCacheDialog = false },
            onDismiss = { showClearCacheDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Surface(
            Modifier.size(30.dp), RoundedCornerShape(9.dp),
            color = Primary().copy(alpha = 0.10f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Primary(), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
    }
}
