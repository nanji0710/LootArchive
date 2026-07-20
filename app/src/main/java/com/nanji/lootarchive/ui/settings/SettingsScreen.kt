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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import com.nanji.lootarchive.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isTabMode: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // 弹窗状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var editReminderDays by remember { mutableStateOf("") }

    Scaffold(
        topBar = { if (!isTabMode) { TopAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } }) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 卡片1：个性化设置 ───
            SectionTitle("个性化设置")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // 显示模式
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("显示模式", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("system" to "跟随", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                            FilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { if (uiState.themeMode != mode) { viewModel.setThemeMode(mode); showThemeDialog = false } },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                // 背景图
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("自定义首页背景图", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { /* TODO: 相册选择 */ }) { Text("上传图片", color = Primary) }
                    TextButton(onClick = { /* TODO: 恢复默认 */ }) { Text("恢复默认", color = TextAuxiliary) }
                }
            }

            // ─── 卡片2：数据管理 ───
            SectionTitle("数据管理")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DataActionButton("导入Excel", Icons.Filled.UploadFile, Modifier.weight(1f)) { /* TODO */ }
                    DataActionButton("导出Excel", Icons.Filled.FileDownload, Modifier.weight(1f)) { /* TODO */ }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DataActionButton("本地备份", Icons.Filled.Backup, Modifier.weight(1f)) { /* TODO */ }
                    DataActionButton("恢复备份", Icons.Filled.Restore, Modifier.weight(1f)) { /* TODO */ }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { if (uiState.trashItemCount > 0) showEmptyTrashDialog = true }) {
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("清理冗余图片 (${uiState.trashItemCount})", fontSize = 14.sp, color = TextAuxiliary)
                }
            }

            // ─── 卡片3：快捷工具 ───
            SectionTitle("快捷工具")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("分类管理", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = TextAuxiliary)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("保修提醒", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("提前 ${uiState.warrantyReminderDays} 天", fontSize = 13.sp, color = TextAuxiliary)
                        IconButton(onClick = { editReminderDays = uiState.warrantyReminderDays.toString(); showReminderDialog = true }) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(16.dp), tint = Primary)
                        }
                    }
                }
            }

            // ─── 卡片4：关于 ───
            SectionTitle("关于拾物集")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("拾物集 ItemGlow", fontSize = 18.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("当前版本 v2.0.3", fontSize = 13.sp, color = TextAuxiliary)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 提醒天数弹窗
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("保修提醒阈值") },
            text = { OutlinedTextField(value = editReminderDays, onValueChange = { editReminderDays = it }, label = { Text("提前天数") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { editReminderDays.toIntOrNull()?.let { viewModel.setWarrantyReminderDays(it) }; showReminderDialog = false }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("取消") } }
        )
    }

    // 清空回收站弹窗
    if (showEmptyTrashDialog) {
        GlassAlertDialog(
            title = "清空冗余数据",
            message = "将删除 ${uiState.trashItemCount} 件已删除物品的关联图片，不可恢复。",
            confirmText = "清空", dismissText = "取消",
            onConfirm = { viewModel.emptyTrash(); showEmptyTrashDialog = false },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun DataActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp)) {
        Icon(icon, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp)
    }
}
