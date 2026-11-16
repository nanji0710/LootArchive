package com.nanji.lootarchive.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.nanji.lootarchive.ui.component.PageTopBar
import com.nanji.lootarchive.ui.component.PageTopBarHeight
import com.nanji.lootarchive.ui.liquidglass.LiquidToggle
import com.nanji.lootarchive.ui.liquidglass.backgroundBrush
import com.nanji.lootarchive.ui.theme.AppIcon
import com.nanji.lootarchive.ui.theme.AppIconVariants
import com.nanji.lootarchive.ui.theme.AppIcons
import com.nanji.lootarchive.ui.theme.AppShape
import com.nanji.lootarchive.ui.theme.AppSpacing
import com.nanji.lootarchive.ui.theme.CardBg
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.LocalGlassColors
import com.nanji.lootarchive.ui.theme.MotionDuration
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextPrimary
import com.nanji.lootarchive.ui.theme.TextSecondary

// ═══════════════════════════════════════════════════════════════
//  设置类页面的行级积木 —— 设置 / 通用设置 / 关于 / 我的 共用一套
//
//  改前：设置页里每一行都是手写的 Row + Surface 图标块 + 分隔线，
//  同样的结构复制了 6 遍，行高、图标块尺寸、右侧留白各不相同；
//  「我的」页又自行长出一套 MyMenuItem，两边改一处要动两遍。
//
//  这里只提供四件事：分组标题、卡片容器、可点行、开关行。
//  所有行共用同一条测量规则（16dp 边距 / 38dp 图标块 / 14dp 图标间距），
//  于是「设置」与「我的」看起来是同一个 App。
// ═══════════════════════════════════════════════════════════════

/** 图标块边长：38dp 是「12dp 垂直内边距 + 图标」的整数解，行高正好落在 62dp。 */
private val IconTileSize = 38.dp

/** 分组标题：图标 + 标题 + 一句说明。 */
@Composable
fun SettingsGroupHeader(
    title: String,
    icon: AppIconVariants? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = AppSpacing.lg, bottom = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Surface(Modifier.size(28.dp), AppShape.thumb, color = Primary().copy(alpha = 0.10f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(icon, null, Modifier.size(16.dp), tint = Primary())
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary(),
                fontFamily = FredokaFont,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary())
            }
        }
    }
}

/** 分组卡片：圆角 20dp、玻璃底 + 顶部高光 + 细描边，分隔线由调用点用 [PreferenceDivider] 插入。 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalGlassColors.current
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                6.dp,
                AppShape.card,
                ambientColor = if (LocalDarkTheme.current) Color.Transparent else Color.White.copy(alpha = 0.35f),
                spotColor = glass.shadow,
            )
            .clip(AppShape.card)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(glass.glassBg.copy(alpha = 0.94f), glass.glassBg.copy(alpha = 0.80f))
                )
            )
            .border(0.5.dp, glass.glassBorder, AppShape.card),
        content = content,
    )
}

/** 行间分隔线：左侧让出图标块宽度，与文字左缘对齐。 */
@Composable
fun PreferenceDivider(startIndent: Boolean = true) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (startIndent) 68.dp else 0.dp, end = 16.dp),
        color = TextAuxiliary().copy(alpha = 0.10f),
    )
}

/** 图标块：所有行共用同一尺寸与底色，切换图标风格时一起变。 */
@Composable
fun IconTile(
    icon: AppIconVariants,
    tint: Color = Primary(),
    size: Dp = IconTileSize,
) {
    Surface(Modifier.size(size), AppShape.thumb, color = tint.copy(alpha = 0.10f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppIcon(icon, null, Modifier.size(size * 0.52f), tint = tint)
        }
    }
}

/**
 * 通用可点行。
 *
 * @param value 右侧摘要文字（如「跟随系统」「提前 7 天」），在箭头之前显示
 * @param showChevron 默认跟随 [onClick]：可点的行才画箭头
 */
@Composable
fun PreferenceRow(
    title: String,
    icon: AppIconVariants? = null,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = Primary(),
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 按下的轻微缩放 + 高光，让设置行也有“玻璃被按到”的反馈，而不是纯文字行。
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(MotionDuration.Quick), label = "rowPress")
    val clickModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .background(if (pressed) Primary().copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(icon, iconTint)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextAuxiliary(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary(),
                maxLines = 1,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(6.dp))
            AppIcon(AppIcons.chevronRight, null, Modifier.size(16.dp), tint = TextAuxiliary())
        }
    }
}

/**
 * 开关行。
 *
 * 玻璃开关要采样「自己所在这一行」作为背景，所以行内容先画进局部捕获层，
 * 开关作为兄弟节点叠在右侧采样它 —— 与旧设置页的做法一致（见 SettingsScreen v5）。
 */
@Composable
fun TogglePreferenceRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: AppIconVariants? = null,
    subtitle: String? = null,
    iconTint: Color = Primary(),
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cardColor = LocalGlassColors.current.cardBg
    val backdrop = rememberLayerBackdrop {
        drawRect(cardColor)
        drawContent()
    }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .layerBackdrop(backdrop)
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(end = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconTile(icon, iconTint)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) TextPrimary() else TextAuxiliary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextAuxiliary(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        LiquidToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
        )
    }
}

/**
 * 单行内嵌一组玻璃分段控件的行（显示模式这种二选一/三选一的场景）。
 *
 * 控件宽度固定 168dp：三段中文标签在 13sp 下正好放得下，
 * 比「标题 + 控件各自随内容伸缩」稳定（旧设置页用 170dp 时偶发换行）。
 */
@Composable
fun SegmentedPreferenceRow(
    title: String,
    icon: AppIconVariants? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    control: @Composable (Backdrop) -> Unit,
) {
    val cardColor = LocalGlassColors.current.cardBg
    val backdrop = rememberLayerBackdrop {
        drawRect(cardColor)
        drawContent()
    }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .layerBackdrop(backdrop)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(end = 184.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconTile(icon, Primary())
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary(),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), maxLines = 1)
                }
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).width(168.dp)) {
            control(backdrop)
        }
    }
}

/** 纯展示的两行文本块（关于页的说明段落）。 */
@Composable
fun PreferenceTextBlock(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = TextPrimary())
        Spacer(Modifier.height(3.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary())
    }
}

/** 卡片容器内的小标题（用于「外观」预览这类需要一块独立区域的场景）。 */
@Composable
fun PreferenceSubtitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = TextAuxiliary(),
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
    )
}

/**
 * 点击就地展开的下拉选择行 —— 与 SleepDown 的 `SleepDownLiquidDropdownPreference` 同一种交互：
 * 点行 → 在行下方展开选项面板（不跳页、不弹全屏对话框），选中即应用并收起。
 *
 * 为什么不用独立页面：外观类开关需要「边看边调」，跳页会把当前上下文（设置列表）
 * 整个换掉，改完还要返回；就地展开只占一块临时区域，点空白/再点行即可收回。
 */
@Composable
fun DropdownPreferenceRow(
    title: String,
    value: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    icon: AppIconVariants? = null,
    subtitle: String? = null,
    iconTint: Color = Primary(),
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var rowWidth by remember { mutableStateOf(0) }
    var rowHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(MotionDuration.Quick),
        label = "dropdownChevron",
    )

    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .onSizeChanged {
                    rowWidth = it.width
                    rowHeight = it.height
                }
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconTile(icon, iconTint)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary(),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextAuxiliary(), maxLines = 1)
                }
            }
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary(), maxLines = 1)
            Spacer(Modifier.width(4.dp))
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = TextAuxiliary(),
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
            )
        }

        // SleepDown 同款：浮在行下方的下拉面板（不是把列表撑开在卡片里）。
        // 用 Popup 脱离父布局，才不会把下面的行整体推下去 —— 视觉上像一块从行里
        // "翻出来"的玻璃菜单，点空白处或再点一次行即收起。
        if (expanded) {
            // 真正的毛玻璃/液态玻璃：Popup 是独立窗口，不会把自己画进页面捕获层，
            // 所以可以安全地采样页面 backdrop —— 透过菜单看到的是被模糊过的下层内容，
            // 而不是一张半透明白板（这是「没有玻璃效果」反馈的根因）。
            val pageBackdrop = com.nanji.lootarchive.ui.liquidglass.LocalPageBackdrop.current
            val dark = com.nanji.lootarchive.ui.theme.LocalDarkTheme.current
            val shape = com.kyant.capsule.ContinuousRoundedRectangle(20.dp)
            Popup(
                // 贴着被点的行下方、右对齐 —— SleepDown 的菜单是从行里"翻出来"的，
                // 不是一张铺满整行的白板。
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, rowHeight),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                val panelModifier = Modifier
                    .width(with(density) { (rowWidth * 0.80f).toDp() })
                    .then(
                        if (pageBackdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = pageBackdrop,
                                shape = { shape },
                                effects = {
                                    colorControls(
                                        brightness = if (dark) 0f else 0.10f,
                                        saturation = 1.35f,
                                    )
                                    blur(if (dark) 12.dp.toPx() else 20.dp.toPx())
                                    lens(20.dp.toPx(), 40.dp.toPx(), depthEffect = true)
                                },
                                highlight = { com.kyant.backdrop.highlight.Highlight.Plain },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = if (dark) 0.10f else 0.34f))
                                },
                            )
                        } else {
                            Modifier
                                .shadow(22.dp, AppShape.card, spotColor = Color.Black.copy(alpha = 0.22f))
                                .clip(AppShape.card)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(
                                            if (dark) Color(0xFF2C2C2E).copy(alpha = 0.97f) else Color.White.copy(alpha = 0.97f),
                                            if (dark) Color(0xFF1C1C1E).copy(alpha = 0.93f) else Color.White.copy(alpha = 0.93f),
                                        )
                                    )
                                )
                        }
                    )
                    .padding(vertical = 6.dp)
                Column(
                    panelModifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEachIndexed { index, option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(index)
                                    expanded = false
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary(),
                                modifier = Modifier.weight(1f),
                            )
                            if (index == selectedIndex) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = TextSecondary(),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一行「标签 + 自定义尾部」的最小容器，供各页拼装特殊行（如版本号、缓存大小）。 */
@Composable
fun PreferenceRowShell(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 设置类二级页的统一骨架：滚动内容 + 悬浮玻璃顶栏。
 *
 * 顶栏必须画在 `layerBackdrop` **之外**（它是采样方，不能同时是被采样方），
 * 而内容要在层内，所以这里把「谁在层里」固定下来，避免每个页面各写一遍、
 * 写错一次就是渲染线程递归崩溃（v7.2.1 的事故原因）。
 */
@Composable
fun SettingsPageScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brush = backgroundBrush()
    val backdrop = rememberLayerBackdrop {
        drawRect(brush = brush)
        drawContent()
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                // statusBarsPadding + 顶栏占位：PageTopBar 自己是 statusBarsPadding 的悬浮层，
                // 内容若只让出 48dp 就会从状态栏底下穿过去（检查更新页初版即踩过）。
                .statusBarsPadding()
                .padding(top = PageTopBarHeight + 8.dp, bottom = 40.dp)
                .padding(horizontal = AppSpacing.pageEdge),
        ) {
            content()
        }
        PageTopBar(
            title = title,
            onNavigateBack = onNavigateBack,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.TopStart),
            trailing = trailing,
        )
    }
}
