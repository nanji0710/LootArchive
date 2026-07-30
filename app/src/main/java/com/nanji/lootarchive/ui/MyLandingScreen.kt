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
    var showExpDialog by remember { mutableStateOf(false) }
    var showAchievementDetail by remember { mutableStateOf<com.nanji.lootarchive.data.local.entity.AchievementEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        // ── v5.5 收藏家卡片（方案A：双行分区）──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                // Row 1: 身份行 — 头像 + 名称 + 收藏等级右对齐
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (LocalDarkTheme.current) Primary().copy(alpha = 0.15f) else Color(0xFFFFEDE0),
                        shadowElevation = 2.dp
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (avatarUri.isNotEmpty()) {
                                AsyncImage(model = Uri.parse(avatarUri), contentDescription = "头像", modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Rounded.Person, null, Modifier.size(36.dp), tint = Primary())
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("拾物集", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                        Text("你的私人物品资产管理工具", fontSize = 12.sp, color = TextAuxiliary())
                    }
                    // 收藏等级 + 成就入口徽章（各自独立显隐，无外层门控）
                    val achCount = profileState.achievements.count { it.isUnlocked }
                    val achTotal = profileState.achievements.size
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (achTotal > 0) {
                            Surface(
                                onClick = { },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF7C3AED).copy(alpha = 0.10f)
                            ) {
                                Text(
                                    "🏅$achCount/$achTotal",
                                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7C3AED),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Separator + EXP 行
                val profile = profileState.profile
                if (profile != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = TextAuxiliary().copy(alpha = 0.12f)
                    )
                    val expProgress = com.nanji.lootarchive.util.ExpCalculator.getLevelProgress(profile.exp)
                    val nextExp = com.nanji.lootarchive.util.ExpCalculator.getNextLevelExp(profile.exp)
                    val currentTitle = com.nanji.lootarchive.util.ExpCalculator.getLevelTitle(profile.level)
                    Surface(
                        onClick = { showExpDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Primary().copy(alpha = 0.05f)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lv.${profile.level} $currentTitle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary())
                                if (profile.level < 10) {
                                    Text("距下一级还需 ${nextExp - profile.exp} EXP", fontSize = 10.sp, color = TextAuxiliary())
                                } else {
                                    Text("已达最高", fontSize = 10.sp, color = Primary())
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { expProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Primary(),
                                trackColor = Primary().copy(alpha = 0.10f)
                            )
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
                                    Surface(
                                        onClick = { showAchievementDetail = ach },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent
                                    ) {
                                        Column(
                                            Modifier.padding(vertical = 4.dp),
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
                Text("当前版本 v6.1.3", fontSize = 13.sp, color = TextAuxiliary())
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
            text = { Text("当前已是最新版本 v6.1.3", color = TextSecondary()) },
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

    // v5.5 EXP 详情弹窗
    if (showExpDialog && profileState.profile != null) {
        val p = profileState.profile!!
        AlertDialog(
            onDismissRequest = { showExpDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("收藏家详情", fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    val expProgress = com.nanji.lootarchive.util.ExpCalculator.getLevelProgress(p.exp)
                    Text("当前等级", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(6.dp))
                    Text("Lv.${p.level} ${com.nanji.lootarchive.util.ExpCalculator.getLevelTitle(p.level)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary())
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { expProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Primary(), trackColor = Primary().copy(alpha = 0.10f)
                    )
                    Spacer(Modifier.height(4.dp))
                    if (p.level < 10) {
                        val nextExp = com.nanji.lootarchive.util.ExpCalculator.getNextLevelExp(p.exp)
                        Text("距下一级还需 ${nextExp - p.exp} EXP", fontSize = 12.sp, color = TextAuxiliary())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("等级数据", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(4.dp))
                    Text("物品新增：${p.totalItemsAdded} 件", fontSize = 13.sp, color = TextSecondary())
                    Text("照片拍摄：${p.totalPhotosAdded} 张", fontSize = 13.sp, color = TextSecondary())
                    Text("描述完善：${p.totalDescriptionsFilled} 件", fontSize = 13.sp, color = TextSecondary())
                    Text("连续活跃：${p.streakDays} 天", fontSize = 13.sp, color = TextSecondary())
                    Spacer(Modifier.height(12.dp))
                    Text("等级阶梯", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(4.dp))
                    com.nanji.lootarchive.util.ExpCalculator.LEVELS.forEachIndexed { i, (exp, title) ->
                        val achieved = p.level >= i + 1
                        Row(Modifier.padding(vertical = 2.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Lv.${i + 1}", fontSize = 11.sp, color = if (achieved) Primary() else TextAuxiliary(), fontFamily = FredokaFont, modifier = Modifier.width(40.dp))
                            Text(title, fontSize = 12.sp, color = if (achieved) TextPrimary() else TextAuxiliary(), fontWeight = if (p.level == i + 1) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.weight(1f))
                            Text(if (exp == 0) "起始" else "${exp} EXP", fontSize = 11.sp, color = TextAuxiliary())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExpDialog = false }) { Text("知道了", color = Primary()) } }
        )
    }

    // v6.0 成就详情弹窗
    showAchievementDetail?.let { ach ->
        val isUnlocked = ach.isUnlocked
        val progressVal = ach.progress
        val targetVal = ach.target
        AlertDialog(
            onDismissRequest = { showAchievementDetail = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isUnlocked) ach.icon.ifEmpty { "🏅" } else "🔒", fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(ach.title, fontWeight = FontWeight.Bold, color = TextPrimary(), fontSize = 20.sp, fontFamily = FredokaFont)
                }
            },
            text = {
                Column {
                    Text(ach.description.ifEmpty { "完成目标即可解锁" }, fontSize = 14.sp, color = TextSecondary())
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("进度", fontSize = 12.sp, color = TextAuxiliary())
                        Text(
                            "$progressVal / $targetVal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isUnlocked) Primary() else TextAuxiliary()
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val prog = (progressVal.toFloat() / targetVal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (isUnlocked) Primary() else TextAuxiliary().copy(alpha = 0.5f),
                        trackColor = Primary().copy(alpha = 0.10f)
                    )
                    Spacer(Modifier.height(8.dp))
                    val statusLabel = if (isUnlocked) "✅ 已解锁" else "🔒 未解锁"
                    Text(statusLabel, fontSize = 13.sp, color = if (isUnlocked) Color(0xFF10B981) else TextAuxiliary(), fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = { TextButton(onClick = { showAchievementDetail = null }) { Text("知道了", color = Primary()) } }
        )
    }

    // v6.0 成就解锁通知
    val unlockMsg = profileState.unlockMessage
    if (unlockMsg != null) {
        LaunchedEffect(unlockMsg) {
            kotlinx.coroutines.delay(2500)
            profileVM.clearUnlockMessage()
        }
        AlertDialog(
            onDismissRequest = { profileVM.clearUnlockMessage() },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("成就解锁！", fontWeight = FontWeight.Bold, color = Primary(), fontSize = 18.sp, fontFamily = FredokaFont) },
            text = { Text(unlockMsg, fontSize = 15.sp, color = TextPrimary()) },
            confirmButton = { TextButton(onClick = { profileVM.clearUnlockMessage() }) { Text("太棒了！", color = Primary()) } }
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
