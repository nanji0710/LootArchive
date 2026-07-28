package com.nanji.lootarchive.ui.backup

import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.data.local.entity.BackupRecordEntity
import com.nanji.lootarchive.ui.component.ClayCard
import com.nanji.lootarchive.ui.component.EmptyState
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
    val context = LocalContext.current

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.fullImport(uri.toString())
    }

    fun launchImport() { importPicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(3000)
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
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, "返回", tint = TextPrimary()) }
                    Text("备份与恢复", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                }
            }
            item { Text("数据备份", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Surface(
                        onClick = { viewModel.fullExport() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(46.dp), RoundedCornerShape(14.dp),
                                color = Primary().copy(alpha = 0.10f)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FileDownload, null, tint = Primary(), modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("一键导出", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary())
                                Text("物品数据 + 照片 + 分类打包为 ZIP", fontSize = 12.sp, color = TextAuxiliary())
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item { Text("数据恢复", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Surface(
                        onClick = { launchImport() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(46.dp), RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.10f)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.UploadFile, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("一键导入", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary())
                                Text("选择备份 ZIP 恢复全部数据", fontSize = 12.sp, color = TextAuxiliary())
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item { Text("备份记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont) }

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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Archive, null, tint = Primary(), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(record.fileName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                                Text(dateFormat.format(Date(record.createdAt)), fontSize = 12.sp, color = TextAuxiliary())
                            }
                            IconButton(onClick = { viewModel.deleteRecord(record) }) {
                                Icon(Icons.Rounded.Delete, "删除", tint = WarrantyExpired, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // 错误/成功弹窗（保持原有逻辑）
        if (uiState.message != null && !uiState.isSuccess) {
            AlertDialog(
                onDismissRequest = { viewModel.clearMessage() },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("操作失败", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text(uiState.message!!, color = TextSecondary(), fontSize = 13.sp) },
                confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("确定", color = Primary()) } }
            )
        }
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary())
            }
        }
        if (uiState.message != null && uiState.isSuccess) {
            AlertDialog(
                onDismissRequest = { viewModel.clearMessage() },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Rounded.CheckCircle, null, tint = Primary()) },
                title = { Text("操作成功", fontWeight = FontWeight.Bold, color = TextPrimary()) },
                text = { Text(uiState.message!!, color = TextSecondary(), fontSize = 14.sp) },
                confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("好的", color = Primary()) } }
            )
        }
    }
}
