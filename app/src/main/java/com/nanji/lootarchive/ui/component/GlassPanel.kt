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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    containerColor: Color = Color.White.copy(alpha = 0.16f),
    borderColor: Color = Color.White.copy(alpha = 0.72f),
    shadowElevation: Dp = 20.dp,
    blurRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(
            HazeTint(Color.White.copy(alpha = 0.12f)),
            HazeTint(Color(0xFFFFF5E8).copy(alpha = 0.04f))
        ),
        blurRadius = blurRadius,
        noiseFactor = 0f,
        fallbackTint = HazeTint(Color.White.copy(alpha = 0.65f))
    )

    Box(
        modifier = modifier
            .shadow(elevation = shadowElevation, shape = shape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .hazeEffect(state = hazeState, style = glassStyle)
            .border(width = 1.dp, color = borderColor, shape = shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content
        )
    }
}
