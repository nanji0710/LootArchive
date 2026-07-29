package com.nanji.lootarchive.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat

import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.ApkDownloadManager
import com.nanji.lootarchive.util.UpdateChecker
import com.nanji.lootarchive.util.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CURRENT_VERSION_CODE: Int get() = com.nanji.lootarchive.BuildConfig.VERSION_CODE

@Composable
fun MyLandingScreen(
    avatarUri: String = "",
    onNavigateToSettings: () -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToRecycleBin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNoUpdate by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(ApkDownloadManager.Progress()) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showLevelDialog by remember { mutableStateOf(false) }

    val downloader = remember { ApkDownloadManager(context) }

    val homeVM: com.nanji.lootarchive.ui.home.HomeViewModel = hiltViewModel()
    val homeState by homeVM.uiState.collectAsState()
    val profileVM: MyLandingViewModel = hiltViewModel()
    val profileState by profileVM.uiState.collectAsState()
    // ── 双维度收藏家等级 ──
    val (levelStars, levelTitle, isValueBadge) = remember(homeState.totalCount, homeState.totalValue) {
        val tc = homeState.totalCount.toDouble()
        val tv = homeState.totalValue

        // 数量线星级
        val countStar = when { tc >= 100 -> 5; tc >= 50 -> 4; tc >= 20 -> 3; tc >= 5 -> 2; tc >= 2 -> 1; else -> 0 }
        // 价值线星级
        val valueStar = when { tv >= 500_000 -> 5; tv >= 100_000 -> 4; tv >= 10_000 -> 3; else -> countStar }

        val finalStar = maxOf(countStar, valueStar)
        val stars = "⭐".repeat(finalStar)
        val valueBadge = valueStar >= finalStar && finalStar >= 3 // 价值驱动
        val title = when (finalStar) {
            5 -> if (valueBadge) "典藏大师" else "博物大家"
            4 -> if (valueBadge) "珍品收藏家" else "藏品达人"
            3 -> if (valueBadge) "资深鉴藏家" else "收纳爱好者"
            2 -> "小小收藏家"
            1 -> "入门收纳家"
            else -> ""
        }
        Triple(stars, title, valueBadge)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── v5.0 收藏家卡片 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (LocalDarkTheme.current) Primary().copy(alpha = 0.15f) else Color(0xFFFFEDE0),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (avatarUri.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(avatarUri),
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.Person, null, Modifier.size(44.dp), tint = Primary())
                        }
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(
                        "拾物集", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary(), fontFamily = FredokaFont
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("你的私人物品资产管理工具", fontSize = 13.sp, color = TextAuxiliary())
                    Spacer(Modifier.height(4.dp))
                    if (levelStars.isNotEmpty()) {
                        Surface(
                            onClick = { showLevelDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Primary().copy(alpha = 0.10f)
                        ) {
                            Text(
                                "${if (isValueBadge) "✨ " else ""}$levelStars $levelTitle",
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = Primary(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    // v5.3 EXP 进度条
                    val profile = profileState.profile
                    if (profile != null && profile.exp > 0) {
                        Spacer(Modifier.height(10.dp))
                        val expProgress = com.nanji.lootarchive.util.ExpCalculator.getLevelProgress(profile.exp)
                        val nextExp = com.nanji.lootarchive.util.ExpCalculator.getNextLevelExp(profile.exp)
                        val nextTitle = com.nanji.lootarchive.util.ExpCalculator.getLevelTitle(profile.level + 1)
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("EXP ${profile.exp}", fontSize = 11.sp, color = TextAuxiliary())
                                Text("Lv.${profile.level} → $nextTitle", fontSize = 11.sp, color = Primary())
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { expProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Primary(),
                                trackColor = Primary().copy(alpha = 0.10f)
                            )
                            if (nextExp < Int.MAX_VALUE) {
                                Spacer(Modifier.height(2.dp))
                                Text("距下一级还需 ${nextExp - profile.exp} EXP", fontSize = 10.sp, color = TextAuxiliary())
                            }
                        }
                    }
                }
            }
        }

        // ── v5.3 成就徽章 ──
        val achievements = profileState.achievements
        if (achievements.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("成就徽章", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, modifier = Modifier.weight(1f))
                        Text("${achievements.count { it.isUnlocked }}/${achievements.size}", fontSize = 13.sp, color = TextAuxiliary())
                    }
                    Spacer(Modifier.height(10.dp))
                    val cols = 3
                    achievements.chunked(cols).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { ach ->
                                Column(
                                    Modifier.weight(1f).padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier.size(40.dp)
                                            .background(
                                                if (ach.isUnlocked) Primary().copy(alpha = 0.10f)
                                                else TextAuxiliary().copy(alpha = 0.06f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (ach.isUnlocked) ach.icon.ifEmpty { "🏅" } else "🔒",
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        ach.title,
                                        fontSize = 10.sp,
                                        color = if (ach.isUnlocked) TextPrimary() else TextAuxiliary(),
                                        fontWeight = if (ach.isUnlocked) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // ── v5.0 收藏亮点 ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 28.sp)
                    Text("最贵物品", fontSize = 11.sp, color = TextAuxiliary(), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                    Text(if (homeState.items.isNotEmpty()) "¥${NumberFormat.getNumberInstance().format(homeState.items.maxByOrNull { it.purchasePrice }?.purchasePrice ?: 0)}" else "暂无", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), textAlign = TextAlign.Center)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", fontSize = 28.sp)
                    Text("最老物品", fontSize = 11.sp, color = TextAuxiliary(), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                    Text(if (homeState.items.any { it.purchaseDate != null }) { val oldest = homeState.items.filter { it.purchaseDate != null }.minByOrNull { it.purchaseDate!! }; java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date(oldest?.purchaseDate!!)) + "年购入" } else "暂无", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), textAlign = TextAlign.Center)
                }
            }
        }

        // ── v5.0 功能入口 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                MyMenuItem(Icons.Rounded.Settings, "设置", "主题模式、提醒", onNavigateToSettings)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Category, "分类管理", "管理物品分类", onNavigateToCategory)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Backup, "备份与恢复", "导出/导入数据", onNavigateToBackup)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Delete, "回收站", "查看和还原已删除物品", onNavigateToRecycleBin)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.SystemUpdate, "检查更新", "检测GitHub最新版本") {
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
        }

        // ── 关于 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("拾物集 ItemGlow", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                Spacer(Modifier.height(4.dp))
                Text("当前版本 v5.3.1", fontSize = 13.sp, color = TextAuxiliary())
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // ...弹窗保持原有逻辑...
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("发现新版本", fontWeight = FontWeight.Bold, color = TextPrimary()) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).fillMaxWidth()) {
                    Text("版本：${updateInfo!!.versionName}", fontSize = 16.sp, color = TextPrimary())
                    Spacer(Modifier.height(4.dp))
                    Text("更新日期：${updateInfo!!.updateDate}", fontSize = 14.sp, color = TextSecondary())
                    Spacer(Modifier.height(8.dp))
                    Text("更新内容：", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                    Text(updateInfo!!.updateLog, fontSize = 13.sp, color = TextSecondary())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = updateInfo!!.apkDownloadUrl
                    val fileName = "LootArchive-v${updateInfo!!.versionName}.apk"
                    if (url.isNotEmpty()) {
                        showUpdateDialog = false; isDownloading = true
                        downloadError = null; downloadProgress = ApkDownloadManager.Progress()
                        scope.launch {
                            val result = downloader.download(url, fileName) { progress -> downloadProgress = progress }
                            result.onSuccess { file ->
                                isDownloading = false
                                if (!downloader.install(file)) downloadError = "无法启动安装器"
                            }.onFailure { e ->
                                isDownloading = false
                                downloadError = "下载失败: ${e.message ?: "未知错误"}"
                            }
                        }
                    } else { Toast.makeText(context, "暂无下载地址", Toast.LENGTH_SHORT).show() }
                }) { Text("下载并安装", color = Primary(), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("取消") } }
        )
    }

    if (showNoUpdate) {
        AlertDialog(
            onDismissRequest = { showNoUpdate = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("已是最新版本", color = TextPrimary()) },
            text = { Text("当前已是最新版本 v5.3.1", color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { showNoUpdate = false }) { Text("好的", color = Primary()) } }
        )
    }

    if (checkError != null) {
        AlertDialog(
            onDismissRequest = { checkError = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("检查失败", color = TextPrimary()) },
            text = { Text("无法连接到更新服务器：${checkError}", color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { checkError = null }) { Text("确定", color = Primary()) } }
        )
    }

    if (isChecking) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("正在检查更新...", color = TextPrimary()) },
            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Primary()) } },
            confirmButton = { }
        )
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("正在下载更新...", fontWeight = FontWeight.Bold, color = TextPrimary()) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth(), color = Primary(),
                        trackColor = Primary().copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(downloadProgress.percentText, fontSize = 14.sp, color = Primary(), fontWeight = FontWeight.Bold)
                        Text(downloadProgress.speedText, fontSize = 12.sp, color = TextAuxiliary())
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(downloadProgress.sizeText, fontSize = 12.sp, color = TextAuxiliary())
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }

    if (downloadError != null) {
        AlertDialog(
            onDismissRequest = { downloadError = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("下载失败", color = TextPrimary()) },
            text = { Text(downloadError!!, color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { downloadError = null }) { Text("确定", color = Primary()) } }
        )
    }

    if (showLevelDialog) {
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("收藏家等级", fontWeight = FontWeight.Bold, color = TextPrimary()) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text("当前进度", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary())
                    Spacer(Modifier.height(8.dp))
                    Text("物品数量：${homeState.totalCount} 件", fontSize = 14.sp, color = TextPrimary())
                    Text("资产总值：¥${java.text.NumberFormat.getNumberInstance().format(homeState.totalValue)}", fontSize = 14.sp, color = TextPrimary())
                    if (isValueBadge) {
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF3E0)) {
                            Text("✨ 珍品收藏家 — 藏品价值卓越", fontSize = 12.sp, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("数量线（收藏广度）", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary())
                    Spacer(Modifier.height(6.dp))
                    LevelRule("⭐", "入门收纳家", "2 件及以上", homeState.totalCount >= 2)
                    LevelRule("⭐⭐", "小小收藏家", "5 件及以上", homeState.totalCount >= 5)
                    LevelRule("⭐⭐⭐", "收纳爱好者", "20 件及以上", homeState.totalCount >= 20)
                    LevelRule("⭐⭐⭐⭐", "藏品达人", "50 件及以上", homeState.totalCount >= 50)
                    LevelRule("⭐⭐⭐⭐⭐", "博物大家", "100 件及以上", homeState.totalCount >= 100)
                    Spacer(Modifier.height(12.dp))
                    Text("价值线（藏品深度）", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary())
                    Spacer(Modifier.height(6.dp))
                    Spacer(Modifier.height(2.dp))
                    LevelRule("⭐⭐⭐", "资深鉴藏家", "总资产 1 万及以上", homeState.totalValue >= 10_000)
                    LevelRule("⭐⭐⭐⭐", "珍品收藏家", "总资产 10 万及以上", homeState.totalValue >= 100_000)
                    LevelRule("⭐⭐⭐⭐⭐", "典藏大师", "总资产 50 万及以上", homeState.totalValue >= 500_000)
                }
            },
            confirmButton = { TextButton(onClick = { showLevelDialog = false }) { Text("知道了", color = Primary()) } }
        )
    }
}

@Composable
private fun MyMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(38.dp), RoundedCornerShape(12.dp),
                color = Primary().copy(alpha = 0.10f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = Primary())
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                Text(subtitle, fontSize = 12.sp, color = TextAuxiliary())
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LevelRule(level: String, title: String, condition: String, achieved: Boolean) {
    Row(modifier = Modifier.padding(vertical = 3.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(level, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text("$title｜$condition", fontSize = 12.sp, color = if (achieved) Primary() else TextAuxiliary(), fontWeight = if (achieved) FontWeight.SemiBold else FontWeight.Normal)
    }
}
