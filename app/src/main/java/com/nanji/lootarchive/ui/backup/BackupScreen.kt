package com.nanji.lootarchive.ui.backup

import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.theme.*
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.component.EmptyState
import com.nanji.lootarchive.ui.component.GlassAlertDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.restoreDatabase(uri.toString())
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importFromExcel(uri.toString())
    }

    // 恢复数据库按钮点击
    fun launchRestore() { restorePicker.launch(arrayOf("application/octet-stream", "application/x-sqlite3")) }
    // 导入Excel按钮点击
    fun launchImport() { importPicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) }

    LaunchedEffect(uiState.message) {
        // 消息显示3秒后自动清除
        if (uiState.message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 返回按钮
            item { Row(Modifier.fillMaxWidth()) { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary()) }; Text("备份与恢复", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary()) } }
            // 备份操作区
            item {
                Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BackupActionButton(
                            icon = Icons.Filled.Backup,
                            title = "备份数据库",
                            subtitle = "导出 SQLite 数据库文件，包含所有物品和分类数据",
                            onClick = { viewModel.backupDatabase() }
                        )
                        BackupActionButton(
                            icon = Icons.Filled.PhotoLibrary,
                            title = "备份照片",
                            subtitle = "将所有物品照片打包为 ZIP 文件",
                            onClick = { viewModel.backupPhotos() }
                        )
                        BackupActionButton(
                            icon = Icons.Filled.FileDownload,
                            title = "导出为 Excel",
                            subtitle = "导出所有物品数据为 Excel 表格，便于查看和编辑",
                            onClick = { viewModel.exportToExcel() }
                        )
                    }
                }
            }

            // 恢复操作区
            item {
                Text("数据恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BackupActionButton(
                            icon = Icons.Filled.Restore,
                            title = "恢复数据库",
                            subtitle = "选择备份文件恢复所有数据（将覆盖现有数据）",
                            onClick = { launchRestore() }
                        )
                        BackupActionButton(
                            icon = Icons.Filled.UploadFile,
                            title = "从 Excel 导入",
                            subtitle = "选择 Excel 文件导入物品数据",
                            onClick = { importPicker.launch(arrayOf("*/*")) }
                        )
                    }
                }
            }

            // 备份记录
            item {
                Text("备份记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (uiState.backupRecords.isEmpty()) {
                item {
                    EmptyState(
                        icon = {
                            Icon(Icons.Filled.History, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        },
                        title = "暂无备份记录",
                        subtitle = "备份数据后将在此显示记录"
                    )
                }
            } else {
                items(uiState.backupRecords, key = { it.id }) { record ->
                    BackupRecordItem(
                        record = record,
                        dateFormat = dateFormat,
                        onDelete = { viewModel.deleteRecord(record) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // 消息提示
        if (uiState.message != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = if (uiState.isSuccess)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ) {
                Text(uiState.message!!)
            }
        }

        // Loading
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun BackupActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun BackupRecordItem(
    record: BackupRecordEntity,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (record.backupType == "database") Icons.Filled.Storage else Icons.Filled.Image,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    dateFormat.format(Date(record.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
