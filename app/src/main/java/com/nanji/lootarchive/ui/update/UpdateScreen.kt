package com.nanji.lootarchive.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.R
import com.nanji.lootarchive.data.update.GitHubReleaseLog
import com.nanji.lootarchive.data.update.GitHubReleaseLogs
import com.nanji.lootarchive.ui.component.IconTile
import com.nanji.lootarchive.ui.liquidglass.LiquidDialog
import com.nanji.lootarchive.ui.theme.AppIcon
import com.nanji.lootarchive.ui.theme.AppIconVariants
import com.nanji.lootarchive.ui.theme.AppIcons
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.LocalGlassColors
import com.nanji.lootarchive.ui.theme.MotionCurve
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextPrimary
import com.nanji.lootarchive.ui.theme.TextSecondary

/**
 * 软件更新页 —— 布局照 SleepDown，背景用拾物集自己的品牌画面。
 *
 * 「检查更新 ↔ 更新说明」的切换也按 SleepDown 的实现：同一页面内用
 * `AnimatedContent(showVersionNotes)` 做「淡入 + 轻微上移 / 淡出」，
 * 而不是瞬间换页。参考 `ImmersiveSoftwareUpdateContent.kt` 的
 * `fadeIn(tween(230, delayMillis = 65)) togetherWith fadeOut(tween(155))`。
 */
private val WarmBgTop: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF0C0C10) else Color(0xFFFFF8EE)
private val WarmBgBottom: Color
    @Composable get() = if (LocalDarkTheme.current) Color(0xFF171118) else Color(0xFFFFE2C0)
private val Ink: Color
    @Composable get() = TextPrimary()
private val InkSoft: Color
    @Composable get() = TextSecondary()

/** 更新页卡片/浮层底色：浅色为白卡，深色跟随玻璃主题的卡片色。 */
@Composable
private fun UpdateCardColor(alpha: Float = 0.96f): Color =
    if (LocalDarkTheme.current) LocalGlassColors.current.cardBg.copy(alpha = alpha)
    else Color.White.copy(alpha = alpha)

/** 日志页的两种模式：本次更新说明 / 全部历史版本日志。 */
private enum class ChangelogMode { CURRENT, HISTORY }

@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val download by viewModel.downloadState.collectAsState()
    val autoCheck by viewModel.autoCheck.collectAsState()
    LaunchedEffect(Unit) { viewModel.autoCheckOnce() }

    var changelogMode by remember { mutableStateOf<ChangelogMode?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = changelogMode,
        transitionSpec = {
            if (targetState != null) {
                // 进入日志页：新页淡入并轻微上移，旧页快速淡出
                (fadeIn(tween(260, delayMillis = 60)) +
                    slideInVertically(tween(320, easing = MotionCurve.Standard)) { it / 12 }) togetherWith
                    fadeOut(tween(150))
            } else {
                // 返回软件更新：反向，新页从上方轻落
                fadeIn(tween(240)) togetherWith
                    (fadeOut(tween(150)) +
                        slideOutVertically(tween(300, easing = MotionCurve.Standard)) { it / 12 })
            }
        },
        label = "updateChangelogSwitch",
    ) { mode ->
        if (mode != null) {
            ChangelogPage(
                onBack = { changelogMode = null },
                showAllVersions = mode == ChangelogMode.HISTORY,
                remoteLog = (uiState as? UpdateUiState.Available)?.info?.updateLog,
            )
        } else {
            UpdateMainPage(
                uiState = uiState,
                download = download,
                autoCheck = autoCheck,
                menuOpen = menuOpen,
                onMenuOpen = { menuOpen = true },
                onMenuDismiss = { menuOpen = false },
                onNavigateBack = onNavigateBack,
                onSetAutoCheck = viewModel::setAutoCheck,
                onOpenAutoUpdateSettings = { showAutoUpdateDialog = true },
                onPrimary = {
                    when (uiState) {
                        is UpdateUiState.Available -> viewModel.download()
                        else -> viewModel.check()
                    }
                },
                onSecondary = { changelogMode = ChangelogMode.CURRENT },
                onHistory = { changelogMode = ChangelogMode.HISTORY },
            )
        }
    }

    if (showAutoUpdateDialog) {
        // 紧凑版：只有「自动检查更新 + 开关」与「完成」，没有标题、副标题和说明，
        // 高度从原来的两段文字 + 图标块收成一行。
        LiquidDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "自动检查更新",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoCheck,
                    onCheckedChange = viewModel::setAutoCheck,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary()),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showAutoUpdateDialog = false }) { Text("完成", color = Primary()) }
            }
        }
    }
}

@Composable
private fun UpdateMainPage(
    uiState: UpdateUiState,
    download: DownloadUiState,
    autoCheck: Boolean,
    menuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onNavigateBack: () -> Unit,
    onSetAutoCheck: (Boolean) -> Unit,
    onOpenAutoUpdateSettings: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onHistory: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WarmBgTop, WarmBgBottom))),
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(520.dp)
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-80).dp)
                .clip(CircleShape)
                .blur(60.dp)
                .alpha(0.30f),
        )
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 120.dp)
                .clip(CircleShape)
                .blur(70.dp)
                .alpha(0.20f),
        )

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", Modifier.size(22.dp), tint = Ink)
                }
                Spacer(Modifier.width(14.dp))
                Text("软件更新", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Box {
                    CircleIconButton(onClick = onMenuOpen) {
                        Icon(Icons.Rounded.MoreVert, "更多选项", Modifier.size(22.dp), tint = Ink)
                    }
                    if (menuOpen) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = androidx.compose.ui.unit.IntOffset(
                                0,
                                with(androidx.compose.ui.platform.LocalDensity.current) { 52.dp.roundToPx() },
                            ),
                            onDismissRequest = onMenuDismiss,
                            properties = PopupProperties(focusable = true),
                        ) {
                            // 与全 App 其它浮层一致的液态玻璃菜单：采样页面 backdrop 做真实模糊，
                            // 而不是一张不透明白卡。Popup 是独立窗口，不会采到自己。
                            val pageBackdrop = com.nanji.lootarchive.ui.liquidglass.LocalPageBackdrop.current
                            val menuShape = com.kyant.capsule.ContinuousRoundedRectangle(18.dp)
                            val menuSurface = if (LocalDarkTheme.current) {
                                Color.White.copy(alpha = 0.12f)
                            } else {
                                Color.White.copy(alpha = 0.40f)
                            }
                            Column(
                                Modifier
                                    .width(180.dp)
                                    .then(
                                        if (pageBackdrop != null) {
                                            Modifier.drawBackdrop(
                                                backdrop = pageBackdrop,
                                                shape = { menuShape },
                                                effects = {
                                                    colorControls(brightness = 0.10f, saturation = 1.30f)
                                                    blur(18.dp.toPx())
                                                    lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                                                },
                                                highlight = { com.kyant.backdrop.highlight.Highlight.Plain },
                                                onDrawSurface = {
                                                    drawRect(menuSurface)
                                                },
                                            )
                                        } else {
                                            Modifier
                                                .shadow(20.dp, menuShape)
                                                .clip(menuShape)
                                                .background(UpdateCardColor(0.96f))
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                            ) {
                                MenuActionRow(AppIcons.history, "历史更新日志") {
                                    onMenuDismiss()
                                    onHistory()
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Ink.copy(alpha = 0.06f),
                                )
                                MenuActionRow(AppIcons.update, "自动更新设置") {
                                    onMenuDismiss()
                                    onOpenAutoUpdateSettings()
                                }
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(BuildConfig.VERSION_NAME, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = InkSoft)
                Spacer(Modifier.height(10.dp))
                Text(
                    "拾物集 ItemGlow",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontFamily = FredokaFont,
                )
            }

            StatusCard(
                uiState = uiState,
                download = download,
                onPrimary = onPrimary,
                onSecondary = onSecondary,
            )
        }
    }
}

@Composable
private fun StatusCard(
    uiState: UpdateUiState,
    download: DownloadUiState,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val title = when (uiState) {
        is UpdateUiState.Checking -> "正在检查更新…"
        is UpdateUiState.UpToDate -> "已是最新版本"
        is UpdateUiState.Available -> "有新版本 ${uiState.info.versionName}"
        is UpdateUiState.Failed -> "检查更新失败"
        else -> "检查更新"
    }
    val subtitle = when (uiState) {
        is UpdateUiState.UpToDate -> BuildConfig.VERSION_NAME
        is UpdateUiState.Available -> "${uiState.info.versionName} · ${uiState.info.updateDate}"
        is UpdateUiState.Failed -> uiState.message
        else -> BuildConfig.VERSION_NAME
    }
    val primaryLabel = when {
        download.installRequested -> "正在打开安装器…"
        uiState is UpdateUiState.Available -> "下载"
        uiState is UpdateUiState.Checking -> "检查中"
        uiState is UpdateUiState.Failed -> "重试"
        else -> "重新检查"
    }
    val enabled = uiState !is UpdateUiState.Checking && !download.running && !download.installRequested

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = UpdateCardColor(0.90f),
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 15.sp, color = InkSoft, maxLines = 2)

            if (!download.error.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    download.error,
                    fontSize = 13.sp,
                    color = if (LocalDarkTheme.current) Color(0xFFFF8A80) else Color(0xFFB3261E),
                )
            }

            Spacer(Modifier.height(20.dp))
            if (download.running) {
                // 下载中：按钮本身变成进度条（填充随进度增长），不再在卡片里再放一条独立进度条
                DownloadProgressPill(download.progress)
            } else {
                PressPillButton(
                    text = primaryLabel,
                    primary = true,
                    enabled = enabled,
                    icon = if (uiState is UpdateUiState.Available) Icons.Rounded.FileDownload else Icons.Rounded.Refresh,
                    onClick = onPrimary,
                )
            }
            Spacer(Modifier.height(12.dp))
            PressPillButton(
                text = "查看更新说明",
                primary = false,
                enabled = true,
                icon = Icons.Rounded.MenuBook,
                onClick = onSecondary,
            )
        }
    }
}

/**
 * 下载进度「按钮」：胶囊本身作为进度条，橙色填充从左向右增长，文字居中显示百分比。
 */
@Composable
private fun DownloadProgressPill(progress: com.nanji.lootarchive.util.ApkDownloadManager.Progress) {
    val fraction by animateFloatAsState(
        targetValue = (progress.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(320),
        label = "downloadProgress",
    )
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(Primary().copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Primary())
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FileDownload, null, Modifier.size(20.dp), tint = Ink)
                Spacer(Modifier.width(8.dp))
                Text(
                    "下载中 ${progress.percentage}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${progress.sizeText} · ${progress.speedText}",
            fontSize = 12.sp,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/**
 * 带按压缩放的胶囊按钮（SleepDown 的 CouiCapsuleButton 同款交互）：
 * 按下 0.97 倍 + 阴影收紧，松开弹回；禁用时降透明度并截断点击。
 */
@Composable
private fun PressPillButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "pressScale",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape)
            .background(if (primary) Primary() else Ink.copy(alpha = 0.08f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp), tint = if (primary) Color.White else Ink)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (primary) Color.White else Ink,
            )
        }
    }
}

/** 「⋮」菜单里的一行图标动作。 */
@Composable
private fun MenuActionRow(
    icon: AppIconVariants,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null, Modifier.size(18.dp), tint = Ink)
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, color = Ink)
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "circlePress",
    )
    Box(
        Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
            .background(UpdateCardColor(0.78f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ChangelogPage(onBack: () -> Unit, showAllVersions: Boolean, remoteLog: String?) {
    var remoteReleases by remember { mutableStateOf<List<GitHubReleaseLog>?>(null) }
    var releaseLoading by remember { mutableStateOf(false) }
    var releaseFailed by remember { mutableStateOf(false) }
    LaunchedEffect(showAllVersions) {
        if (showAllVersions && remoteReleases == null) {
            releaseLoading = true
            GitHubReleaseLogs.fetch()
                .onSuccess { remoteReleases = it }
                .onFailure { releaseFailed = true }
            releaseLoading = false
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(WarmBgTop, WarmBgBottom)))) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(460.dp)
                .align(Alignment.TopEnd)
                .offset(x = 140.dp, y = (-100).dp)
                .clip(CircleShape)
                .blur(60.dp)
                .alpha(0.24f),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", Modifier.size(22.dp), tint = Ink)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    if (showAllVersions) "历史更新日志" else "更新说明",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                if (showAllVersions) {
                    if (releaseLoading && remoteReleases == null) {
                        Text(
                            "正在从 GitHub 获取发版日志…",
                            fontSize = 13.sp,
                            color = InkSoft,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    val remoteEntries = remoteReleases
                        ?.map { it.toVersionChangelog() }
                        .orEmpty()
                    val entries = (remoteEntries + VersionChangelogs.filter { local ->
                        remoteEntries.none { it.version == local.version }
                    }).sortedWith { a, b -> compareVersion(b.version, a.version) }
                    VersionHistoryList(entries)
                    if (releaseFailed && remoteReleases == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "暂时连不上 GitHub，先显示内置日志。",
                            fontSize = 12.sp,
                            color = InkSoft,
                        )
                    }
                } else {
                    CurrentVersionCard(remoteLog)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CurrentVersionCard(remoteLog: String?) {
    var notesExpanded by remember { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (notesExpanded) 0f else 180f,
        animationSpec = tween(220),
        label = "changelogChevron",
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = UpdateCardColor(0.92f),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("软件版本", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(BuildConfig.VERSION_NAME, fontSize = 16.sp, color = InkSoft)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Ink.copy(alpha = 0.08f),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { notesExpanded = !notesExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("更新说明", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Ink.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = if (notesExpanded) "收起" else "展开",
                        modifier = Modifier.size(20.dp).rotate(chevronRotation),
                        tint = Ink,
                    )
                }
            }
            AnimatedVisibility(
                visible = notesExpanded,
                enter = expandVertically(tween(300), expandFrom = Alignment.Top) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(260), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    if (!remoteLog.isNullOrBlank()) {
                        remoteLog.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                "• ${line.trim().trimStart('-', '•', ' ')}",
                                fontSize = 15.sp,
                                lineHeight = 26.sp,
                                color = InkSoft,
                                modifier = Modifier.padding(vertical = 5.dp),
                            )
                        }
                    } else {
                        VersionChangelogs.first().sections.forEach { (section, bullets) ->
                            SectionBlock(section, bullets)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionHistoryList(entries: List<VersionChangelog>) {
    entries.forEachIndexed { index, entry ->
        var expanded by remember(entry.version) { mutableStateOf(index == 0) }
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 0f else 180f,
            animationSpec = tween(220),
            label = "versionChevron",
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = UpdateCardColor(0.92f),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("v${entry.version}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(entry.date, fontSize = 12.sp, color = InkSoft)
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Ink.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = if (expanded) "收起" else "展开",
                            modifier = Modifier.size(19.dp).rotate(rotation),
                            tint = Ink,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(300), expandFrom = Alignment.Top) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(260), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
                ) {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
                        entry.sections.forEach { (section, bullets) -> SectionBlock(section, bullets) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionBlock(section: String, bullets: List<String>) {
    Text(
        section,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Ink,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    bullets.forEach { bullet ->
        Text(
            "• $bullet",
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = InkSoft,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

private data class VersionChangelog(
    val version: String,
    val date: String,
    val sections: List<Pair<String, List<String>>>,
)

/** GitHub Release 的 body（Markdown）转成页面用的分段结构。 */
private fun GitHubReleaseLog.toVersionChangelog(): VersionChangelog {
    val lines = body.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            line.removePrefix("- ").removePrefix("* ").removePrefix("# ").trim()
        }
    return VersionChangelog(
        version = version,
        date = date,
        sections = listOf("更新内容" to lines),
    )
}

/** 8.0.1 / 8.0.0 / 7.5.5 这种版本号按数字比较，避免字符串排序把 8.0.1 排到 7.x 后面。 */
private fun compareVersion(a: String, b: String): Int {
    val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
    val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
    val size = maxOf(pa.size, pb.size)
    for (i in 0 until size) {
        val diff = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

/** 全部历史版本日志：菜单「历史更新日志」展示这里的所有版本。 */
private val VersionChangelogs = listOf(
    VersionChangelog(
        "8.0.8", "2026-11-17",
        listOf(
            "更新提醒" to listOf(
                "每次启动 App 自动检查更新。",
                "有更新时，底栏设置按钮和设置页应用卡显示小红点。",
                "关于应用页显示「发现新版本」，点击进入更新页直接看到新版本和下载按钮。",
            ),
        ),
    ),
    VersionChangelog(
        "8.0.7", "2026-11-16",
        listOf(
            "深色模式" to listOf(
                "设置页图标改为统一的主题配色，顶部应用图标新增深色版本。",
                "检查更新页、更新说明和历史更新日志的页面背景、卡片、文字全部跟随主题切换。",
                "设置卡片阴影和图标底色在深色模式下不再使用白色。",
            ),
            "AI 助手与稳定性" to listOf(
                "新会话不再串用上一条物品的上下文，打开历史会话时也会清空旧的操作上下文。",
                "历史记录写入失败不会阻断回答；长期记忆检索异常时按无记忆处理。",
                "修复修改购买日期时保修到期日没有跟着变化的问题。",
                "修复模型返回 JSON 带代码块或 null 时解析失败的问题。",
            ),
            "其他优化" to listOf(
                "下载失败会清理半成品 APK，下载百分比限制在 0–100。",
                "日期格式化改为线程安全实现，避免后台任务与页面同时格式化时出错。",
                "数据库 v8→v9 升级的表结构与 Room 期望保持一致，避免升级校验失败。",
            ),
        ),
    ),
    VersionChangelog(
        "8.0.6", "2026-11-16",
        listOf(
            "AI 助手" to listOf(
                "修复改日期时误改名称，日期和名称按字段分别处理。",
                "配置了 API 时优先使用模型的语义解析，模型返回无效动作时不会再静默套用本地分类规则。",
                "模型生成的新增/修改结果会标注「模型回答」，方便确认实际走的是 API。",
                "继续保留最近对话和操作上下文，支持「它/这个/刚才那个」指代。",
            ),
            "历史更新日志" to listOf("继续直接读取 GitHub Releases，发版后自动出现。"),
        ),
    ),
    VersionChangelog(
        "8.0.5", "2026-11-16",
        listOf(
            "上下文记忆" to listOf(
                "新增操作上下文：记住最近操作的物品、分类、位置。",
                "用户说「它/这个/刚才那个」时能定位到上一条物品，改日期、名称、分类时按字段识别。",
                "长期记忆会保存常用分类、常用位置和用户偏好，之后聊天自动参考。",
                "修复把日期当成名称修改的问题。",
            ),
            "历史更新日志" to listOf("继续直接读取 GitHub Releases，发版后自动出现。"),
        ),
    ),
    VersionChangelog(
        "8.0.4", "2026-11-16",
        listOf(
            "AI 上下文记忆" to listOf(
                "新增长期记忆：会从对话里提取并保存你的偏好、固定事实，之后聊天自动参考。",
                "保留最近对话和刚新增的物品，改日期、名称、分类时能识别对应字段，不再把日期当成名称。",
                "新增物品时分类交给模型根据你的真实分类列表选择。",
            ),
            "历史更新日志" to listOf("继续直接读取 GitHub Releases，发版后自动出现。"),
        ),
    ),
    VersionChangelog(
        "8.0.3", "2026-11-16",
        listOf(
            "AI 上下文记忆" to listOf(
                "改日期时不会再误改名称，能理解「今年11月」「26年11月」等说法。",
                "新增物品时分类由模型根据你的分类列表选择，不再写死分类关键词。",
            ),
            "历史更新日志" to listOf("继续直接读取 GitHub Releases，发版后自动出现。"),
        ),
    ),
    VersionChangelog(
        "8.0.2", "2026-11-16",
        listOf(
            "历史更新日志" to listOf(
                "历史更新日志改为直接读取 GitHub Releases。",
                "以后每次发版，App 会自动拿到这一版的日志，不再依赖本地写死的版本记录。",
                "即使本地没有录入新版本，发版后也能在历史日志里看到。",
            ),
            "AI 助手上下文理解和记忆" to listOf(
                "AI 助手会记住最近的对话和刚新增的物品。",
                "名字写错了、价格或分类要改时，直接说「改成…」就能找到对应物品并修改。",
                "新增物品时的分类交给模型根据名称从你的分类列表里选，不再写死分类关键词。",
            ),
        ),
    ),
    VersionChangelog(
        "8.0.1", "2026-11-16",
        listOf(
            "修复" to listOf(
                "下载进度条改为从左往右填充，不再从中间向两边展开。",
                "更新说明和历史更新日志补上 8.0.0 的完整记录。",
                "修复输入法弹出、收起时输入框和聊天内容被遮挡、跳动的问题。",
                "关闭「启用 AI 助理」后，底栏助手按钮会立即隐藏。",
                "AI 思考强度直接使用接口返回的档位（low / high / max 等）。",
                "修复 OpenCode Go 接口缺少 x-opencode-session 导致的 HTTP 400。",
            ),
        ),
    ),
    VersionChangelog(
        "8.0.0", "2026-11-16",
        listOf(
            "设置与界面" to listOf(
                "设置页重新整理，常用入口集中到一页；点顶部应用卡可以查看版本、检查更新。",
                "新增视效模式、图标风格切换；卡片、菜单、弹窗统一成玻璃效果。",
                "「我的」页改为「设置」，底栏新增独立圆形新增按钮。",
            ),
            "AI 助理" to listOf(
                "新增 AI 助理：可以问东西放哪、值多少钱、哪些快过期。",
                "支持一句话记物品：说清名称、价格、购买时间、保质期等，就能自动录入。",
                "聊天记录保存在手机本地，随时能翻看；历史沟通里可以勾选删除。",
                "AI 设置：填好接口地址和 API Key 后能拉取全部可用模型，思考强度按接口返回的档位选择。",
            ),
            "检查更新" to listOf(
                "检查更新页重做：有新版本时按钮变「下载」。",
                "下载时按钮本身变成进度条，从左往右填充，能看到百分比和速度。",
                "更新说明按版本整理成历史日志，旧版本也能查。",
            ),
            "修复与优化" to listOf(
                "修复若干已知问题，优化页面切换动画和部分机型的显示问题。",
            ),
        ),
    ),
    VersionChangelog(
        "7.5.5", "2026-10-16",
        listOf(
            "界面与交互" to listOf(
                "重做设置页：大标题、搜索、应用卡与彩色图标行；回收站、检查更新等入口各自只保留一处。",
                "「我的」页改为「设置」，底栏新增独立圆形新增按钮，与导航胶囊同处一行。",
                "软件更新页：版本卡、检查状态、更新日志与下载进度在同一页完成。",
            ),
            "动画与视觉" to listOf(
                "首页「新增物品」按钮改为从按钮位置向四周撑开的扩散动画，返回时原路收回。",
                "页面打开/返回统一为共享元素「从哪来到哪去」，标题与金额逐帧插值。",
            ),
            "修复" to listOf("修复若干已知问题。"),
        ),
    ),
    VersionChangelog(
        "7.5.0", "2026-09-20",
        listOf(
            "界面与交互" to listOf(
                "二级页返回按钮统一为玻璃款，设置页补顶栏，关于页顶栏对齐。",
                "打开/返回改为共享元素「从哪来到哪去」，首页固定 UI 联动位移，字号连续插值。",
            ),
        ),
    ),
    VersionChangelog(
        "7.2.2", "2026-08-18",
        listOf("修复" to listOf("修复 v7.2.1 启动闪退。")),
    ),
    VersionChangelog(
        "7.2.1", "2026-08-15",
        listOf(
            "动画" to listOf("页面返回动画重做：整屏平移 + 前后页面连贯衔接。"),
        ),
    ),
)