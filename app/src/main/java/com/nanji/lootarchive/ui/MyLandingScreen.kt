package com.nanji.lootarchive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.ApkDownloadManager
import com.nanji.lootarchive.util.UpdateChecker
import com.nanji.lootarchive.util.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CURRENT_VERSION_CODE = 63

@Composable
fun MyLandingScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNoUpdate by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }

    // 下载进度状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(ApkDownloadManager.Progress()) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val downloader = remember { ApkDownloadManager(context) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头像和信息区
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Primary().copy(alpha = 0.15f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, Modifier.size(36.dp), tint = Primary())
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("拾物集", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary())
                    Text("你的私人物品资产管理工具", fontSize = 13.sp, color = TextAuxiliary())
                }
            }
        }

        // 快捷入口卡片
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            MyMenuItem(Icons.Outlined.Settings, "设置", "主题模式、提醒、数据备份") { onNavigateToSettings() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glassBorderColor())
            MyMenuItem(Icons.Outlined.Category, "分类管理", "管理物品分类") { onNavigateToCategory() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glassBorderColor())
            MyMenuItem(Icons.Outlined.Backup, "备份与恢复", "导出Excel、备份数据") { onNavigateToBackup() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glassBorderColor())
            MyMenuItem(Icons.Outlined.SystemUpdate, "检查更新", "检测GitHub最新版本") {
                if (!isChecking) {
                    isChecking = true
                    scope.launch {
                        try {
                            val result = UpdateChecker.check(CURRENT_VERSION_CODE)
                            result.onSuccess { info ->
                                if (info != null) { updateInfo = info; showUpdateDialog = true }
                                else { showNoUpdate = true }
                            }.onFailure { e -> checkError = e.message }
                        } catch (e: Exception) { checkError = e.message }
                        isChecking = false
                    }
                }
            }
        }

        // 关于
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("拾物集 ItemGlow", fontSize = 18.sp, color = TextPrimary())
            Spacer(Modifier.height(4.dp))
            Text("当前版本 v2.7.6", fontSize = 13.sp, color = TextAuxiliary())
        }
    }

    // 更新弹窗
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("发现新版本", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("版本：${updateInfo!!.versionName}", fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("更新日期：${updateInfo!!.updateDate}", fontSize = 14.sp, color = TextSecondary())
                    Spacer(Modifier.height(8.dp))
                    Text("更新内容：", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(updateInfo!!.updateLog, fontSize = 13.sp, color = TextSecondary())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = updateInfo!!.apkDownloadUrl
                    val fileName = "LootArchive-v${updateInfo!!.versionName}.apk"
                    if (url.isNotEmpty()) {
                        showUpdateDialog = false
                        isDownloading = true
                        downloadError = null
                        downloadProgress = ApkDownloadManager.Progress()
                        scope.launch {
                            val result = downloader.download(url, fileName) { progress ->
                                downloadProgress = progress
                            }
                            result.onSuccess { file ->
                                isDownloading = false
                                val installed = downloader.install(file)
                                if (!installed) {
                                    downloadError = "无法启动安装器，请检查系统设置"
                                }
                            }.onFailure { e ->
                                isDownloading = false
                                downloadError = e.message ?: "下载失败"
                            }
                        }
                    } else {
                        Toast.makeText(context, "暂无下载地址", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("下载并安装", color = Primary()) }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("取消") }
            }
        )
    }

    // 已是最新
    if (showNoUpdate) {
        AlertDialog(
            onDismissRequest = { showNoUpdate = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("已是最新版本") },
            text = { Text("当前已是最新版本 v2.7.6") },
            confirmButton = { TextButton(onClick = { showNoUpdate = false }) { Text("好的") } }
        )
    }

    // 检查失败
    if (checkError != null) {
        AlertDialog(
            onDismissRequest = { checkError = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("检查失败") },
            text = { Text("无法连接到更新服务器：${checkError}") },
            confirmButton = { TextButton(onClick = { checkError = null }) { Text("确定") } }
        )
    }

    // 检查中
    if (isChecking) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("正在检查更新...") },
            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } },
            confirmButton = { }
        )
    }

    // 下载进度弹窗
    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("正在下载更新...", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary(),
                        trackColor = Primary().copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(downloadProgress.percentText, fontSize = 14.sp, color = Primary(), fontWeight = FontWeight.Bold)
                        Text(downloadProgress.speedText, fontSize = 12.sp, color = TextAuxiliary())
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(downloadProgress.sizeText, fontSize = 12.sp, color = TextAuxiliary())
                    if (downloadProgress.percentage == 100) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary())
                            Spacer(Modifier.width(8.dp))
                            Text("正在准备安装...", fontSize = 13.sp, color = TextAuxiliary())
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }

    // 下载失败弹窗
    if (downloadError != null) {
        AlertDialog(
            onDismissRequest = { downloadError = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("下载失败") },
            text = { Text(downloadError!!) },
            confirmButton = { TextButton(onClick = { downloadError = null }) { Text("确定") } }
        )
    }
}

@Composable
private fun MyMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = Primary())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                Text(subtitle, fontSize = 13.sp, color = TextAuxiliary())
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextAuxiliary())
        }
    }
}
