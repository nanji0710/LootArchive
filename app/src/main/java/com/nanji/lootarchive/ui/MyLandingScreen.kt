package com.nanji.lootarchive.ui

import androidx.compose.foundation.layout.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.ui.component.topFade
import com.nanji.lootarchive.ui.nav.NavKeys
import com.nanji.lootarchive.ui.nav.navSharedElement

@Composable
fun MyLandingScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val homeVM: com.nanji.lootarchive.ui.home.HomeViewModel = hiltViewModel()
    val homeState by homeVM.uiState.collectAsState()

    // 滚动状态提到外面：顶栏渐隐要读它的滚动量（见 ui/component/ScrollFade.kt）
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).topFade(scrollState).statusBarsPadding().navigationBarsPadding().padding(horizontal = AppSpacing.pageEdge, vertical = AppSpacing.pageVertical),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        Text("我的", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        // ── v5.5 收藏家卡片（方案A：双行分区）──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShape.sheet,
            colors = CardDefaults.cardColors(
                containerColor = CardBg()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                // Row 1: 身份行 — 头像 + 名称
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = AppShape.card,
                        color = if (LocalDarkTheme.current) Primary().copy(alpha = 0.15f) else AvatarWarmLight,
                        shadowElevation = 2.dp
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, Modifier.size(36.dp), tint = Primary())
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("拾物集", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary(), fontFamily = FredokaFont)
                        Text("你的私人物品资产管理工具", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
                    }
                }
            }
        }

        // ── v5.0 收藏亮点 ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = AppShape.card,
                colors = CardDefaults.cardColors(containerColor = CardBg()),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(28.dp), tint = Primary())
                    Spacer(Modifier.height(4.dp))
                    Text("最贵物品", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                    Text(if (homeState.items.isNotEmpty()) "¥${NumberFormat.getNumberInstance().format(homeState.items.maxByOrNull { it.purchasePrice }?.purchasePrice ?: 0)}" else "暂无", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary(), textAlign = TextAlign.Center)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = AppShape.card,
                colors = CardDefaults.cardColors(containerColor = CardBg()),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(28.dp), tint = TextAuxiliary())
                    Spacer(Modifier.height(4.dp))
                    Text("最老物品", style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                    Text(if (homeState.items.any { it.purchaseDate != null }) { val oldest = homeState.items.filter { it.purchaseDate != null }.minByOrNull { it.purchaseDate!! }; if (oldest?.purchaseDate != null) java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date(oldest.purchaseDate)) + "年购入" else "暂无" } else "暂无", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary(), textAlign = TextAlign.Center)
                }
            }
        }

        // ── v5.0 功能入口 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShape.card,
            colors = CardDefaults.cardColors(containerColor = CardBg()),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                MyMenuItem(Icons.Rounded.Settings, "设置", "主题模式、提醒", onNavigateToSettings, NavKeys.SETTINGS)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Category, "分类管理", "管理物品分类", onNavigateToCategory, NavKeys.CATEGORY)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Backup, "备份与恢复", "导出/导入数据", onNavigateToBackup, NavKeys.BACKUP)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Delete, "回收站", "查看和还原已删除物品", onNavigateToRecycleBin, NavKeys.RECYCLEBIN)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = TextAuxiliary().copy(alpha = 0.10f))
                MyMenuItem(Icons.Rounded.Info, "关于", "版本信息、检查更新", onNavigateToAbout, NavKeys.ABOUT)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun MyMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    /** 与目标二级页配对的共享元素 key：飞出去的就是这一行本身。 */
    sharedKey: String
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().navSharedElement(sharedKey), color = Color.Transparent) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(38.dp), AppShape.thumb,
                color = Primary().copy(alpha = 0.10f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = Primary())
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = TextPrimary())
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextAuxiliary(), modifier = Modifier.size(18.dp))
        }
    }
}
