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
import androidx.compose.material.icons.filled.*
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
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 个性化 ──
            SectionHeader(Icons.Filled.Palette, "个性化")
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                // 显示模式 — 卡片式选择
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示模式", fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("system" to "跟随", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                            FilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { if (uiState.themeMode != mode) viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary().copy(alpha = 0.15f),
                                    selectedLabelColor = Primary()
                                )
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = TextAuxiliary().copy(alpha = 0.12f))
                // 自定义头像
                Row(
                    Modifier.fillMaxWidth().clickable { avatarPicker.launch("image/*") }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自定义头像", fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                    if (uiState.avatarUri.isNotEmpty()) {
                        Surface(
                            onClick = { viewModel.setAvatarUri("") },
                            shape = RoundedCornerShape(8.dp),
                            color = Primary().copy(alpha = 0.1f)
                        ) {
                            Text("还原", fontSize = 12.sp, color = Primary(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextAuxiliary())
                }
            }

            // ── 提醒 ──
            SectionHeader(Icons.Filled.Notifications, "提醒")
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("保修到期提醒", fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("提前 ${uiState.warrantyReminderDays} 天", fontSize = 14.sp, color = TextAuxiliary())
                        IconButton(onClick = {
                            editReminderDays = uiState.warrantyReminderDays.toString()
                            showReminderDialog = true
                        }) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(18.dp), tint = Primary())
                        }
                    }
                }
            }

            // ── 存储 ──
            SectionHeader(Icons.Filled.Storage, "存储")
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("缓存大小", fontSize = 16.sp, color = TextPrimary())
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary())
                        } else {
                            Icon(Icons.Filled.DeleteSweep, null, Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("清除缓存", fontSize = 13.sp)
                    }
                }
            }

            // ── 关于 ──
            SectionHeader(Icons.Filled.Info, "关于")
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Text("拾物集 ItemGlow", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                Spacer(Modifier.height(4.dp))
                Text("当前版本 v4.0.6", fontSize = 13.sp, color = TextAuxiliary())
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 弹窗
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("保修提醒阈值") },
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
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("取消") } }
        )
    }

    if (showEmptyTrashDialog) {
        GlassAlertDialog(
            title = "清空冗余数据",
            message = "将删除 ${uiState.trashItemCount} 件已删除物品的关联图片，不可恢复。",
            confirmText = "清空", dismissText = "取消",
            onConfirm = { viewModel.emptyTrash(); showEmptyTrashDialog = false },
            onDismiss = { showEmptyTrashDialog = false }
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
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    ) {
        Surface(
            Modifier.size(30.dp), RoundedCornerShape(9.dp),
            color = Primary().copy(alpha = 0.12f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Primary(), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
    }
}
