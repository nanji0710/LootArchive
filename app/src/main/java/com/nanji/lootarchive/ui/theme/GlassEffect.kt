package com.nanji.lootarchive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * v5.0 Glass 规格 — 毛玻璃效果层级
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

// ═══════════════════════════════════════════════════════════════
//  v5.0: 全局 HazeState — 让所有子组件都能访问玻璃模糊
// ═══════════════════════════════════════════════════════════════
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * v5.0 玻璃态效果 Modifier
 * Light: 微阴影 + 白色半透明 = 轻盈玻璃感
 * Dark:  微亮边 + 深色半透明 = 从深色中浮出
 */
@Composable
fun Modifier.glassEffect(
    tier: GlassTier = GlassTier.CARD
): Modifier {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(tier.cornerRadiusDp.dp)
    return this.shadow(
        elevation = tier.shadowElevationDp.dp,
        shape = shape,
        ambientColor = glass.highlight,
        spotColor = glass.shadow
    )
}
