package com.nanji.lootarchive.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.R
import com.nanji.lootarchive.ui.component.*
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.ui.update.UpdateUiState
import com.nanji.lootarchive.ui.update.UpdateViewModel

/**
 * 关于应用 —— 对照 SleepDown 的「关于应用」页：
 * 顶部大图（应用名 + 版本 + 更新状态，整块可点，进入「软件更新」），
 * 下面依次是开发者/仓库、隐私与许可、项目致谢。
 *
 * 与检查更新的关系：关于是入口，更新是详情。关于页的大图就是 SleepDown 里
 * 那张「点进去看软件更新」的 hero。
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpdate: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val updateState by updateViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { updateViewModel.autoCheckOnce() }

    SettingsPageScaffold(title = "关于应用", onNavigateBack = onNavigateBack) {

        // ── Hero：大图 + 应用名 + 版本 + 更新状态（点击进软件更新）──
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2B1B12),
                            Color(0xFF6B3A1E),
                            Color(0xFFE8782A),
                        )
                    )
                )
                .clickable(onClick = onNavigateToUpdate),
        ) {
            // 柔光装饰：两团半透明圆，模拟 SleepDown 大图里的光晕
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 70.dp)
                    .clip(RoundedCornerShape(110.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Box(
                Modifier
                    .size(140.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-40).dp, y = (-30).dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (LocalDarkTheme.current) R.mipmap.ic_launcher_dark else R.mipmap.ic_launcher
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp)),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "拾物集 ItemGlow",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FredokaFont,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(14.dp))
                SurfaceChip(
                    text = when (val s = updateState) {
                        is UpdateUiState.Available -> "发现新版本 ${s.info.versionName}"
                        is UpdateUiState.Checking -> "正在检查更新…"
                        is UpdateUiState.Failed -> "检查更新失败"
                        is UpdateUiState.UpToDate -> "已是最新版本"
                        else -> "点击查看更新"
                    },
                    dark = true,
                )
            }
        }

        // 模块间距：大图与信息卡之间留 20dp，卡片与底部文案之间留 24dp
        Spacer(Modifier.height(20.dp))

        // ── 开发者 / 仓库 ──
        SettingsCard {
            PreferenceRow(
                title = "关于开发者",
                icon = AppIcons.person,
                subtitle = "nanji0710",
                onClick = { runCatching { uriHandler.openUri("https://github.com/nanji0710") } },
            )
            PreferenceDivider()
            PreferenceRow(
                title = "项目仓库",
                icon = AppIcons.link,
                subtitle = "LootArchive",
                onClick = { runCatching { uriHandler.openUri("https://github.com/nanji0710/LootArchive") } },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "把喜欢的东西，收进自己的档案里",
            style = MaterialTheme.typography.labelSmall,
            color = TextAuxiliary(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SurfaceChip(text: String, dark: Boolean = false) {
    androidx.compose.material3.Surface(
        shape = AppShape.pill,
        color = if (dark) Color.White.copy(alpha = 0.18f) else Primary().copy(alpha = 0.12f),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (dark) Color.White else Primary(),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}
