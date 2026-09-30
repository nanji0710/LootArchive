package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.LocalReduceMotion
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextSecondary

/**
 * 液态玻璃筛选胶囊 —— 首页分类筛选等。选中=主色淡底+主色字；未选中=玻璃透明底+次级字。
 */
@Composable
fun LiquidFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    label: String,
    height: Dp = 32.dp,
    horizontalPadding: Dp = 14.dp
) {
    val dark = LocalDarkTheme.current
    val primary = Primary()
    val containerColor =
        if (selected) primary.copy(alpha = 0.15f)
        else if (dark) Color(0xFF121212).copy(alpha = 0.25f)
        else Color(0xFFFFFFFF).copy(alpha = 0.35f)
    val animationScope = rememberCoroutineScope()
    // 减弱动效时按压高光不再跟手（见 InteractiveHighlight.reduceMotion）
    val reduceMotion = LocalReduceMotion.current
    val highlight = remember(animationScope, reduceMotion) { InteractiveHighlight(animationScope, reduceMotion = reduceMotion) }
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = { vibrancy(); blur(2.dp.toPx()); lens(12.dp.toPx(), 24.dp.toPx()) },
                layerBlock = {
                    val progress = highlight.pressProgress
                    val scale = lerp(1f, 1f + 1.dp.toPx() / size.height, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .height(height)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) primary else TextSecondary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
