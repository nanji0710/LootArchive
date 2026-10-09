package com.nanji.lootarchive.ui.backup

import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.data.repository.BackupRepository.Companion.DOWNLOAD_SUBDIR
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.util.Feedback
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 弹窗开关。注意：弹窗内容一律读下面捕获好的局部非空值，不再在 content lambda
    // 里读 uiState.xxx!!（详见文件末尾的弹窗说明）。
    var showExportConfirm by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupRecordEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<BackupRecordEntity?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.fullImport(uri.toString())
    }

    fun launchImport() { importPicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }

    // 消息弹窗读的是捕获下来的快照，而不是随时会变 null 的 uiState.message
    val message = uiState.message
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(Feedback.BACKUP_DISMISS)
            viewModel.clearMessage()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary()) }
                    Text("备份与恢复", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                }
            }
            item { Text("数据备份", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }
            item {
                val exportBrush = backgroundBrush()
                val exportBackdrop = rememberLayerBackdrop {
                    drawRect(brush = exportBrush)
                    drawContent()
                }
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .layerBackdrop(exportBackdrop)
                    )
                    LiquidGlassButton(
                        onClick = { showExportConfirm = true },
                        backdrop = exportBackdrop,
                        modifier = Modifier.fillMaxWidth(),
                        height = 80.dp,
                        horizontalPadding = 18.dp
                    ) {
                        Surface(
                            Modifier.size(46.dp), AppShape.panel,
                            color = Primary().copy(alpha = 0.10f)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.FileDownload, null, tint = Primary(), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("一键导出", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary())
                            Text("物品数据 + 照片 + 分类打包为 ZIP", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                    }
                }
            }

            item { Text("数据恢复", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }
            item {
                val importBrush = backgroundBrush()
                val importBackdrop = rememberLayerBackdrop {
                    drawRect(brush = importBrush)
                    drawContent()
                }
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .layerBackdrop(importBackdrop)
                    )
                    LiquidGlassButton(
                        onClick = { launchImport() },
                        backdrop = importBackdrop,
                        modifier = Modifier.fillMaxWidth(),
                        height = 80.dp,
                        horizontalPadding = 18.dp
                    ) {
                        Surface(
                            Modifier.size(46.dp), AppShape.panel,
                            color = Color(0xFF10B981).copy(alpha = 0.10f)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.UploadFile, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("一键导入", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary())
                            Text("选择备份 ZIP 恢复全部数据", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                    }
                }
            }

            item { Text("备份记录", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }

            if (uiState.backupRecords.isEmpty()) {
                item {
                    EmptyState(
                        icon = { Icon(Icons.Rounded.History, null, modifier = Modifier.size(48.dp), tint = TextAuxiliary()) },
                        title = "暂无备份记录",
                        subtitle = "备份数据后将在此显示记录"
                    )
                }
            } else {
                items(uiState.backupRecords, key = { it.id }) { record ->
                    Card(
                        onClick = { pendingRestore = record },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShape.panel,
                        colors = CardDefaults.cardColors(containerColor = CardBg()),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Archive, null, tint = Primary(), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(record.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary())
                                Text(dateFormat.format(Date(record.createdAt)), style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
                                Text("点击恢复此备份", style = MaterialTheme.typography.labelSmall, color = Primary())
                            }
                            IconButton(onClick = { pendingDelete = record }) {
                                Icon(Icons.Rounded.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // ────────────────────────────────────────────────────────────────
        // 弹窗
        //
        // 这里所有弹窗内容都只读"当场捕获的局部非空值"，绝不在 content lambda 里
        // 写 uiState.xxx!!。原因：LiquidAlertDialog 不是就地组合的，它把内容注册进
        // LiquidDialogHostState，由 MainScreen 的 LiquidDialogHost 渲染；退场动画
        // 期间宿主仍然持有并调用这份 content（LiquidDialog.kt 的 retainedEntry）。
        // 于是"点确认 → 清空 uiState.message → 弹窗退场"这一瞬间，宿主会再调一次
        // content，而那时的 uiState.message 已经是 null，`!!` 直接抛
        // KotlinNullPointerException 闪退。捕获成局部val后，退场时读到的仍是旧快照。
        // ────────────────────────────────────────────────────────────────

        if (showExportConfirm) {
            // 快照：confirm 回调与 content 都闭包在这份值上，不依赖后续 recomposition
            val exportHint = "将导出 物品数据 + 照片 + 分类 为 ZIP 压缩包。"
            LiquidAlertDialog(
                onDismissRequest = { showExportConfirm = false },
                icon = { Icon(Icons.Rounded.FileDownload, null, tint = Primary()) },
                title = { Text("一键导出", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text("$exportHint\n保存位置: Download/$DOWNLOAD_SUBDIR/", color = TextSecondary(), style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    TextButton(onClick = {
                        showExportConfirm = false
                        viewModel.fullExport()
                    }) { Text("确认导出", color = Primary()) }
                },
                dismissButton = { TextButton(onClick = { showExportConfirm = false }) { Text("取消", color = TextSecondary()) } }
            )
        }

        if (pendingRestore != null) {
            val restoreName = pendingRestore!!.fileName
            LiquidAlertDialog(
                onDismissRequest = { pendingRestore = null },
                icon = { Icon(Icons.Rounded.SettingsBackupRestore, null, tint = Primary()) },
                title = { Text("恢复备份", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text("将从「$restoreName」恢复数据。\n备份里的物品会覆盖同名物品，不会重复添加；你现在多出来的物品会保留。", color = TextSecondary(), style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    TextButton(onClick = {
                        val record = pendingRestore
                        pendingRestore = null
                        if (record != null) viewModel.restoreRecord(record)
                    }) { Text("确认恢复", color = Primary()) }
                },
                dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("取消", color = TextSecondary()) } }
            )
        }

        if (pendingDelete != null) {
            val deleteName = pendingDelete!!.fileName
            LiquidAlertDialog(
                onDismissRequest = { pendingDelete = null },
                icon = { Icon(Icons.Rounded.DeleteOutline, null, tint = WarrantyExpired) },
                title = { Text("删除备份记录", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text("「$deleteName」的记录和备份文件都会被删除，无法恢复。", color = TextSecondary(), style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    TextButton(onClick = {
                        val record = pendingDelete
                        pendingDelete = null
                        if (record != null) viewModel.deleteRecord(record)
                    }) { Text("删除", color = WarrantyExpired) }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消", color = TextSecondary()) } }
            )
        }

        if (message != null && !uiState.isSuccess) {
            val errorText = message
            LiquidAlertDialog(
                onDismissRequest = { viewModel.clearMessage() },
                icon = { Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("操作失败", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text(errorText, color = TextSecondary(), style = MaterialTheme.typography.bodySmall) },
                confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("确定", color = Primary()) } }
            )
        }
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary())
            }
        }
        if (message != null && uiState.isSuccess) {
            val successText = message
            LiquidAlertDialog(
                onDismissRequest = { viewModel.clearMessage() },
                icon = { Icon(Icons.Rounded.CheckCircle, null, tint = Primary()) },
                title = { Text("操作成功", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text(successText, color = TextSecondary(), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("好的", color = Primary()) } }
            )
        }
    }
}
