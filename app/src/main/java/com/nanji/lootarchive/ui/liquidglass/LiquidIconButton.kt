package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 圆形液态玻璃图标按钮 —— 返回/编辑/删除等 40dp 小圆钮。
 */
@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.35f)
        else Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val animationScope = rememberCoroutineScope()
    // 减弱动效时按压高光不再跟手（见 InteractiveHighlight.reduceMotion）
    val reduceMotion = LocalReduceMotion.current
    val highlight = remember(animationScope, reduceMotion) { InteractiveHighlight(animationScope, reduceMotion = reduceMotion) }
    Box(
        modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = { vibrancy(); blur(2.dp.toPx()); lens(12.dp.toPx(), 24.dp.toPx()) },
                layerBlock = {
                    if (size.value > 0f) {
                        val progress = highlight.pressProgress
                        val scale = lerp(1f, 1f + 2.dp.toPx() / size.toPx(), progress)
                        scaleX = scale
                        scaleY = scale
                    }
                },
                onDrawSurface = {
                    if (tint.isSpecified) drawRect(tint)
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
            .then(highlight.gestureModifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}
