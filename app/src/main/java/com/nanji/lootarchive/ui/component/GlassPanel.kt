package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * 拟物底部导航面板
 *
 * Light: 浅灰底 + 双层阴影（上亮下暗）→ 从页面中浮起
 * Dark:  深灰底 + 微弱亮边 → 在深色背景上凸显
 *
 * 保留 Haze 模糊能力以提供层级感，但降低强度适配拟物风格
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    containerColor: Color = Color(0xFFEEF0F4),
    borderColor: Color = Color.White.copy(alpha = 0.65f),
    shadowElevation: Dp = 10.dp,
    blurRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(containerColor.copy(alpha = 0.55f)),
            HazeTint(Color.White.copy(alpha = 0.04f))
        ),
        blurRadius = blurRadius,
        noiseFactor = 0f,
        fallbackTint = HazeTint(containerColor)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.White.copy(alpha = 0.70f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .shadow(
                elevation = 1.dp,
                shape = shape,
                ambientColor = Color.White.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = glassStyle)
            .background(containerColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content
        )
    }
}
