package com.nanji.lootarchive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * v5.0 Glass 规格 — 毛玻璃效果层级
 * 从 Neumorphism 双层阴影 → 单层微阴影 + 玻璃边框
 */
enum class GlassTier(
    val cornerRadiusDp: Int,
    val shadowElevationDp: Int,
    val blurRadiusDp: Int
) {
    NAV(24, 8, 24),
    CARD(20, 4, 20),
    DIALOG(28, 12, 30),
    FAB(28, 8, 14),
    SHEET(28, 12, 30)
}

/**
 * v5.0 玻璃态效果 Modifier
 * Light: 微阴影 + 白色半透明 = 轻盈玻璃感
 * Dark:  微亮边 + 深色半透明 = 从深色中浮出
 */
@Composable
fun Modifier.glassEffect(
    tier: GlassTier = GlassTier.CARD
): Modifier {
    val dark = LocalDarkTheme.current
    val shape = when (tier) {
        GlassTier.FAB -> RoundedCornerShape(tier.cornerRadiusDp.dp)
        else -> RoundedCornerShape(tier.cornerRadiusDp.dp)
    }

    val shadowColor = if (dark)
        Color.Black.copy(alpha = 0.30f)
    else
        Color.Black.copy(alpha = 0.06f)

    val highlightColor = if (dark)
        Color.White.copy(alpha = 0.03f)
    else
        Color.White.copy(alpha = 0.70f)

    return this.shadow(
        elevation = tier.shadowElevationDp.dp,
        shape = shape,
        ambientColor = highlightColor,
        spotColor = shadowColor
    )
}

// ── v5.0 边框颜色 helpers ──
@Composable
fun glassBorderColor() = if (LocalDarkTheme.current)
    Color.White.copy(alpha = 0.08f)
else
    Color.White.copy(alpha = 0.55f)

@Composable
fun glassBackground() = if (LocalDarkTheme.current)
    _GlassDark
else
    _GlassLight

@Composable
fun navGlassBackground() = if (LocalDarkTheme.current)
    _NavGlassDark
else
    _NavGlassLight

// ── v5.0 阴影色 ──
@Composable
fun glassShadowColor() = if (LocalDarkTheme.current)
    Color.Black.copy(alpha = 0.30f)
else
    Color.Black.copy(alpha = 0.05f)

// ── 向后兼容：保留 neumorphRaised 签名但不使用多层阴影 ──
@Composable
fun Modifier.neumorphRaised(
    tier: GlassTier = GlassTier.CARD
): Modifier = this.glassEffect(tier)
