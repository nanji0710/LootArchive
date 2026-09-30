package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.LocalReduceMotion
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 液态玻璃按钮 —— vibrancy + blur(2dp) + lens(12,24) + 按压缩放/位移 + RuntimeShader 高光。
 * Android 13+ 全玻璃；12/12L 由 drawBackdrop 内部降级为实体。
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    pressExpansion: Dp = 4.dp,
    highlightIntensity: Float = 1f,
    highlightRadiusMultiplier: Float = 1.5f,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.40f)
        else Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val animationScope = rememberCoroutineScope()
    // 减弱动效时按压高光不再跟手（见 InteractiveHighlight.reduceMotion）
    val reduceMotion = LocalReduceMotion.current
    val highlight = remember(animationScope, reduceMotion) {
        InteractiveHighlight(
            animationScope,
            intensity = highlightIntensity,
            radiusMultiplier = highlightRadiusMultiplier,
            reduceMotion = reduceMotion
        )
    }
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = { vibrancy(); blur(2.dp.toPx()); lens(12.dp.toPx(), 24.dp.toPx()) },
                layerBlock = {
                    val width = size.width
                    val heightPx = size.height
                    if (width > 0f && heightPx > 0f) {
                        val progress = highlight.pressProgress
                        val scale = lerp(1f, 1f + pressExpansion.toPx() / heightPx, progress)
                        val maxOffset = size.minDimension
                        val offset = highlight.offset
                        if (maxOffset > 0f) {
                            translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                            translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                        }
                        val maxDragScale = pressExpansion.toPx() / heightPx
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / heightPx).coerceAtMost(1f)
                        scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (heightPx / width).coerceAtMost(1f)
                    }
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = tint.alpha * 0.75f))
                    }
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                    drawRect(containerColor)
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
