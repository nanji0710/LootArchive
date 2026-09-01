package com.nanji.lootarchive.ui.about

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.R
import com.nanji.lootarchive.ui.component.GlassSurface
import com.nanji.lootarchive.ui.liquidglass.LiquidAlertDialog
import com.nanji.lootarchive.ui.liquidglass.LiquidGlassButton
import com.nanji.lootarchive.ui.liquidglass.LiquidIconButton
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.ApkDownloadManager
import com.nanji.lootarchive.util.UpdateChecker
import com.nanji.lootarchive.util.UpdateInfo
import com.nanji.lootarchive.util.isValidVersionName
import kotlinx.coroutines.launch

private val CURRENT_VERSION_CODE: Int get() = com.nanji.lootarchive.BuildConfig.VERSION_CODE

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    // ── 更新状态（从 MyLandingScreen 迁移，逐字保留）──
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNoUpdate by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(ApkDownloadManager.Progress()) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val downloader = remember { ApkDownloadManager(context) }

    val bgBrush = backgroundBrush()
    val backdrop = rememberLayerBackdrop {
        drawRect(brush = bgBrush)
        drawContent()
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbsBackground(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶栏：标题（返回按钮为层外悬浮兄弟，避免自采样崩溃）
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).padding(start = 52.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("关于", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))

                // Logo 区
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = null
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("拾物集 ItemGlow", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                    Spacer(Modifier.height(4.dp))
                    Text("当前版本 v${BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = TextAuxiliary())
                }
                Spacer(Modifier.height(16.dp))

                // 链接卡
                GlassSurface {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri("https://github.com/nanji0710/LootArchive") } }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), color = Primary().copy(alpha = 0.10f)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Code, null, Modifier.size(20.dp), tint = Primary())
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("GitHub 仓库", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                                Text("开源代码、提交反馈", fontSize = 12.sp, color = TextAuxiliary())
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 信息卡
                GlassSurface {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        AboutTextItem("使用说明", "纯本地私人物品资产管理工具。所有数据仅保存在本机数据库与本地文件中，可通过备份功能导出 ZIP 存档。")
                        HorizontalDivider(color = TextAuxiliary().copy(alpha = 0.10f))
                        AboutTextItem("数据隐私", "应用不联网上传任何数据；仅“检查更新”功能会请求 GitHub API 查询版本信息，不包含个人数据。")
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        // 返回按钮：层外兄弟（安全采样，与层内标题行分开）
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 8.dp)
        ) {
            LiquidIconButton(onClick = onNavigateBack, backdrop = backdrop) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = TextPrimary(), modifier = Modifier.size(20.dp))
            }
        }

        // 底部：检查更新玻璃按钮
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            LiquidGlassButton(
                onClick = {
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
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp,
                enabled = !isChecking
            ) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp), tint = Primary())
                Spacer(Modifier.width(8.dp))
                Text(if (isChecking) "正在检查..." else "检查更新", color = Primary(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── 更新弹窗（从 MyLandingScreen 逐字迁移）──
    if (showUpdateDialog && updateInfo != null) {
        LiquidAlertDialog(
            onDismissRequest = { showUpdateDialog = false },
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
                    val safeVersion = updateInfo!!.versionName.takeIf { isValidVersionName(it) }
                    val fileName = safeVersion?.let { "LootArchive-v$it.apk" } ?: return@TextButton
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
        LiquidAlertDialog(
            onDismissRequest = { showNoUpdate = false },
            title = { Text("已是最新版本", color = TextPrimary()) },
            text = { Text("当前已是最新版本 v${BuildConfig.VERSION_NAME}", color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { showNoUpdate = false }) { Text("好的", color = Primary()) } }
        )
    }

    if (checkError != null) {
        LiquidAlertDialog(
            onDismissRequest = { checkError = null },
            title = { Text("检查失败", color = TextPrimary()) },
            text = { Text("无法连接到更新服务器：${checkError}", color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { checkError = null }) { Text("确定", color = Primary()) } }
        )
    }

    if (isChecking) {
        LiquidAlertDialog(
            onDismissRequest = {},
            title = { Text("正在检查更新...", color = TextPrimary()) },
            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Primary()) } },
            confirmButton = { }
        )
    }

    if (isDownloading) {
        LiquidAlertDialog(
            onDismissRequest = {},
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
        LiquidAlertDialog(
            onDismissRequest = { downloadError = null },
            title = { Text("下载失败", color = TextPrimary()) },
            text = { Text(downloadError!!, color = TextSecondary()) },
            confirmButton = { TextButton(onClick = { downloadError = null }) { Text("确定", color = Primary()) } }
        )
    }
}

@Composable
private fun AboutTextItem(title: String, desc: String) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 13.sp, color = TextSecondary())
    }
}

/**
 * 动态光斑背景 —— 暖色光斑缓慢漂移（自研，全 API 级别可用）。
 */
@Composable
private fun AnimatedOrbsBackground(modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val transition = rememberInfiniteTransition(label = "orbs")
    val driftX by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftX"
    )
    val driftY by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftY"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulse"
    )
    val amber = Color(0xFFE8782A)
    val purple = Color(0xFF7C3AED)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.28f
        // 光斑1：暖琥珀（左上↔右下漂移）
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(amber.copy(alpha = 0.16f + 0.04f * pulse), Color.Transparent)
                else listOf(amber.copy(alpha = 0.14f + 0.05f * pulse), Color.Transparent),
                center = Offset(w * (0.25f + 0.2f * driftX), h * (0.2f + 0.15f * driftY)),
                radius = r
            ),
            radius = r,
            center = Offset(w * (0.25f + 0.2f * driftX), h * (0.2f + 0.15f * driftY))
        )
        // 光斑2：优雅紫（右下↔左上漂移，相位相反）
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(purple.copy(alpha = 0.12f + 0.03f * (1f - pulse)), Color.Transparent)
                else listOf(purple.copy(alpha = 0.10f + 0.04f * (1f - pulse)), Color.Transparent),
                center = Offset(w * (0.75f - 0.18f * driftX), h * (0.75f - 0.15f * driftY)),
                radius = r
            ),
            radius = r,
            center = Offset(w * (0.75f - 0.18f * driftX), h * (0.75f - 0.15f * driftY))
        )
        // 光斑3：柔白/柔金（中部小光斑）
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(amber.copy(alpha = 0.06f + 0.03f * pulse), Color.Transparent)
                else listOf(Color(0xFFFFF3E0).copy(alpha = 0.10f + 0.04f * pulse), Color.Transparent),
                center = Offset(w * (0.5f + 0.12f * driftX), h * (0.5f + 0.1f * driftY)),
                radius = w * 0.18f
            ),
            radius = w * 0.18f,
            center = Offset(w * (0.5f + 0.12f * driftX), h * (0.5f + 0.1f * driftY))
        )
    }
}
