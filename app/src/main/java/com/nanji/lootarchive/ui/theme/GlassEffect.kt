package com.nanji.lootarchive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 苹果毛玻璃效果参数 — 4 档规格（文档 Section 1.2）
 */
enum class GlassTier(
    val blurRadiusDp: Int,    // 模糊半径（文档标注，实际用透明度模拟）
    val alphaLight: Float,
    val alphaDark: Float,
    val cornerRadiusDp: Int,
    val shadowElevationDp: Int
) {
    /** 顶部导航栏 / 底部 Tab 栏 / 侧边抽屉 */
    NAV(12, 0.88f, 0.78f, 28, 4),

    /** 数据卡片 / 物品网格卡片 / 功能横条 */
    CARD(16, 0.86f, 0.76f, 22, 4),

    /** 弹窗 / 图表详情浮层 / 筛选下拉面板 */
    DIALOG(18, 0.84f, 0.74f, 24, 8),

    /** 悬浮圆形新增按钮 */
    FAB(14, 0.90f, 0.80f, 50, 10)
}

/**
 * 获取当前模式下的玻璃背景色
 */
@Composable
fun glassBackground(tier: GlassTier): Color {
    val dark = LocalDarkTheme.current
    val base = if (dark) _GlassBgDark else _GlassBgLight
    val alpha = if (dark) tier.alphaDark else tier.alphaLight
    return base.copy(alpha = alpha.coerceIn(0f, 1f))
}

@Composable
fun glassBorderColor(): Color =
    if (LocalDarkTheme.current) _GlassBorderDark else _GlassBorderLight

/**
 * 应用玻璃效果的 Modifier — 统一使用透明度模拟毛玻璃
 */
@Composable
fun Modifier.glassEffect(
    tier: GlassTier = GlassTier.CARD,
    withShadow: Boolean = true,
    withBorder: Boolean = true
): Modifier {
    val bg = glassBackground(tier)
    val corner = if (tier == GlassTier.FAB) 50 else tier.cornerRadiusDp
    val shape: Shape = if (tier == GlassTier.FAB)
        RoundedCornerShape(50)
    else
        RoundedCornerShape(corner.dp)

    return this
        .then(
            if (withShadow) Modifier.shadow(tier.shadowElevationDp.dp, shape, ambientColor = bg.copy(alpha = 0.22f))
            else Modifier
        )
        .clip(shape)
        .background(bg)
        .then(
            if (withBorder) Modifier.border(1.dp, glassBorderColor(), shape)
            else Modifier
        )
}
