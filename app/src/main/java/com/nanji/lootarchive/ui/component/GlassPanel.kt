package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme._NavGlassDark
import com.nanji.lootarchive.ui.theme._NavGlassLight
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * v5.0 玻璃导航面板 — 浮动胶囊式
 *
 * Light: 高透明白 + backdrop 模糊 + 微边框 = 漂浮玻璃
 * Dark:  深色半透明 + backdrop 模糊 + 微亮边 = 深色玻璃
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    containerColor: Color = if (LocalDarkTheme.current) _NavGlassDark else _NavGlassLight,
    borderColor: Color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.50f),
    shadowElevation: Dp = 8.dp,
    blurRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalDarkTheme.current

    val glassStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(containerColor),
            HazeTint(if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.08f))
        ),
        blurRadius = blurRadius,
        noiseFactor = 0f,
        fallbackTint = HazeTint(containerColor)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = if (dark) Color.Black.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.04f),
                spotColor = if (dark) Color.Black.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.04f)
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = glassStyle)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content
        )
    }
}
