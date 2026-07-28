package com.nanji.lootarchive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 拟物光影规格 — 控制凸起/凹陷深度感
 */
enum class GlassTier(
    val cornerRadiusDp: Int,
    val shadowElevationDp: Int
) {
    NAV(20, 10),
    CARD(18, 6),
    DIALOG(20, 12),
    FAB(50, 14)
}

/**
 * 拟物凸起效果 Modifier
 * Light: 亮面左上 + 暗影右下 = 凸起
 * Dark:  微弱亮面 + 深暗影 = 从深色中浮出
 */
@Composable
fun Modifier.neumorphRaised(
    tier: GlassTier = GlassTier.CARD
): Modifier {
    val dark = LocalDarkTheme.current
    val shape = if (tier == GlassTier.FAB)
        RoundedCornerShape(50)
    else
        RoundedCornerShape(tier.cornerRadiusDp.dp)

    val highlight = if (dark) 0x08 else 0xE6
    val shadowAlpha = if (dark) 0x50 else 0x10

    return this
        .shadow(tier.shadowElevationDp.dp, shape, ambientColor = androidx.compose.ui.graphics.Color.White.copy(alpha = highlight.toFloat() / 255f))
        .shadow((tier.shadowElevationDp / 3).dp, shape, spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = shadowAlpha.toFloat() / 255f))
}

// 向后兼容
@Composable
fun glassBorderColor() = if (LocalDarkTheme.current)
    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
else
    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f)

@Composable
fun glassBackground(tier: GlassTier) = if (LocalDarkTheme.current)
    _NavGlassDark
else
    _NavGlassLight

@Composable
fun Modifier.glassEffect(
    tier: GlassTier = GlassTier.CARD,
    withShadow: Boolean = true,
    withBorder: Boolean = false
): Modifier = this.neumorphRaised(tier)
