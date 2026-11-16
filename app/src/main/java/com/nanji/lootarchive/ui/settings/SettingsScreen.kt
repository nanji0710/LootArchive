package com.nanji.lootarchive.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.R
import com.nanji.lootarchive.ui.component.PreferenceDivider
import com.nanji.lootarchive.ui.component.SettingsCard
import com.nanji.lootarchive.ui.theme.*

/**
 * 设置页 —— 按 SleepDown 的设置页重建：大标题 + 搜索框 + 分组卡片 + 彩色图标导航行。
 *
 * 关键取舍（与上一版的差异）：
 * 1. **设置就是一级页**：底部导航的第四项直接叫「设置」，不再经过「我的」中转。
 *    上一版「我的」里又列了一遍回收站/检查更新/关于，和设置页重复，用户要判断
 *    「这两个入口是不是同一个东西」——现在全 App 每个功能只有一个入口。
 * 2. **分组卡片不写小标题**：分组本身就是语义（通用 / AI / 关于），
 *    再给每组加一行「⚙ 通用」标题只是多一层视觉噪音（SleepDown 同款做法）。
 * 3. **行内只有标题**：说明文字留给二级页，设置首页负责"找得到"，不负责"讲清楚"。
 * 4. **搜索**：设置项多起来之后，搜索比记住分组更快；匹配标题与关键词。
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    isTabMode: Boolean = true,
    onNavigateToCategory: () -> Unit = {},
    onNavigateToGeneral: () -> Unit = {},
    onNavigateToAssistantSettings: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val groups = remember(
        onNavigateToGeneral, onNavigateToCategory, onNavigateToBackup, onNavigateToRecycleBin,
        onNavigateToAssistantSettings, onNavigateToAssistant,
    ) {
        listOf(
            SettingsGroup(
                rows = listOf(
                    SettingsEntry("通用设置", "价格 货币 保修 提醒 备份 缓存 单位 外观 视效 图标", AppIcons.settings, IosBlue, onNavigateToGeneral),
                    SettingsEntry("分类管理", "分类 新增 重命名 排序", AppIcons.category, IosOrange, onNavigateToCategory),
                    SettingsEntry("备份与恢复", "备份 恢复 ZIP 导出 导入", AppIcons.backup, IosGreen, onNavigateToBackup),
                    SettingsEntry("回收站", "删除 还原 清空", AppIcons.trash, IosPurple, onNavigateToRecycleBin),
                )
            ),
            SettingsGroup(
                rows = listOf(
                    SettingsEntry("AI 设置", "AI 助理 模型 接口 Key 开关", AppIcons.assistant, IosPink, onNavigateToAssistantSettings),
                    SettingsEntry("AI 助理", "助手 对话 问答 库存", AppIcons.chat, IosTeal, onNavigateToAssistant),
                )
            ),
        )
    }

    val filtered = remember(query, groups) {
        if (query.isBlank()) groups
        else groups.mapNotNull { group ->
            val hits = group.rows.filter { row ->
                row.title.contains(query, ignoreCase = true) || row.keywords.contains(query, ignoreCase = true)
            }
            if (hits.isEmpty()) null else SettingsGroup(hits)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(if (isTabMode) 18.dp else 56.dp))

            if (isTabMode) {
                Text(
                    "设置",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary(),
                    fontFamily = FredokaFont,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                )
            }

            // ── 搜索：设置项变多后，搜索比记分组更快 ──
            SearchField(
                value = query,
                onValueChange = { query = it },
                onClear = { query = "" },
                onSearch = { keyboard?.hide() },
            )

            // 应用卡：名称 + 版本，点击进入「关于应用」（关于页的大图再进软件更新）
            SettingsCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToAbout)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "拾物集 ItemGlow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary(),
                            fontFamily = FredokaFont,
                        )
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextAuxiliary(),
                        )
                    }
                    AppIcon(AppIcons.chevronRight, null, Modifier.size(16.dp), tint = TextAuxiliary())
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    "没有找到相关设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextAuxiliary(),
                    modifier = Modifier.padding(16.dp),
                )
            }

            filtered.forEach { group ->
                SettingsCard {
                    group.rows.forEachIndexed { index, row ->
                        if (index > 0) PreferenceDivider(startIndent = true)
                        SettingsNavRow(row)
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

private data class SettingsEntry(
    val title: String,
    val keywords: String,
    val icon: AppIconVariants,
    val tint: Color,
    val onClick: () -> Unit,
)

private data class SettingsGroup(val rows: List<SettingsEntry>)

/** iOS 系统色板：SleepDown 设置页同款，彩色图标才有"可扫读"的分组感。 */
private val IosBlue = Color(0xFF0A84FF)
private val IosOrange = Color(0xFFFF9500)
private val IosGreen = Color(0xFF34C759)
private val IosPurple = Color(0xFF5856D6)
private val IosPink = Color(0xFFFF2D55)
private val IosTeal = Color(0xFF00A9C7)
private val IosIndigo = Color(0xFF5E5CE6)
private val IosGray = Color(0xFF8E8E93)

/**
 * 设置导航行：彩色图标 + 标题 + 箭头，52dp 行高。
 *
 * 与旧行的差别：不再给图标套 38dp 的圆角色块 —— 色块会让一列 5 行变成 5 个
 * 互相竞争的小方块；SleepDown 的做法是「图标本身着色、行底透明」，
 * 视觉重心回到文字上，扫读更快。
 */
@Composable
private fun SettingsNavRow(row: SettingsEntry) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "navRowPress",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = row.onClick)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .background(if (pressed) Primary().copy(alpha = 0.05f) else Color.Transparent)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(row.icon, null, Modifier.size(22.dp), tint = row.tint)
        Spacer(Modifier.width(16.dp))
        Text(
            row.title,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppIcon(AppIcons.chevronRight, null, Modifier.size(16.dp), tint = TextAuxiliary().copy(alpha = 0.7f))
    }
}

/** 圆角搜索框：与 SleepDown 设置页一致的「先搜索、后分组」入口。 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CardBg().copy(alpha = 0.94f),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(50), spotColor = TextAuxiliary().copy(alpha = 0.25f))
            .border(0.5.dp, TextAuxiliary().copy(alpha = 0.12f), RoundedCornerShape(50)),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = TextAuxiliary())
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary()),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary()),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("搜索设置项", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary())
                    }
                    inner()
                },
            )
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "清除",
                    tint = TextAuxiliary(),
                    modifier = Modifier.size(18.dp).clickable(onClick = onClear),
                )
            }
        }
    }
}
