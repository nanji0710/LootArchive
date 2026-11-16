package com.nanji.lootarchive.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.MotionCurve
import com.nanji.lootarchive.ui.theme.MotionDuration

/**
 * 暖色光斑背景 —— 「沉浸」视效模式下的全局环境动效。
 *
 * 两团低透明度暖色光斑以不同相位缓慢漂移，让玻璃背后的画布"活着"。
 * 只在 `VisualEffectMode.IMMERSIVE` 下挂载（由调用点判断）：一旦组合，
 * 这层 infinite transition 会持续请求帧，省电/均衡模式不应该付这份成本。
 */
@Composable
fun AmbientOrbsBackground(modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val transition = rememberInfiniteTransition(label = "ambientOrbs")
    // 环境动效不是交互，时长自成一套（见 MotionDuration.Ambient）。
    val driftX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MotionDuration.Ambient.OrbMedium, easing = MotionCurve.Linear),
            RepeatMode.Reverse,
        ),
        label = "orbsDriftX",
    )
    val driftY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MotionDuration.Ambient.OrbSlow, easing = MotionCurve.Linear),
            RepeatMode.Reverse,
        ),
        label = "orbsDriftY",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MotionDuration.Ambient.OrbFast, easing = MotionCurve.EaseInOut),
            RepeatMode.Reverse,
        ),
        label = "orbsPulse",
    )
    val amber = Color(0xFFE8782A)
    val purple = Color(0xFF7C3AED)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.28f
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(amber.copy(alpha = 0.16f + 0.04f * pulse), Color.Transparent)
                else listOf(amber.copy(alpha = 0.14f + 0.05f * pulse), Color.Transparent),
                center = Offset(w * (0.25f + 0.2f * driftX), h * (0.2f + 0.15f * driftY)),
                radius = r,
            ),
            radius = r,
            center = Offset(w * (0.25f + 0.2f * driftX), h * (0.2f + 0.15f * driftY)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(purple.copy(alpha = 0.12f + 0.03f * (1f - pulse)), Color.Transparent)
                else listOf(purple.copy(alpha = 0.10f + 0.04f * (1f - pulse)), Color.Transparent),
                center = Offset(w * (0.75f - 0.18f * driftX), h * (0.75f - 0.15f * driftY)),
                radius = r,
            ),
            radius = r,
            center = Offset(w * (0.75f - 0.18f * driftX), h * (0.75f - 0.15f * driftY)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(amber.copy(alpha = 0.06f + 0.03f * pulse), Color.Transparent)
                else listOf(Color(0xFFFFF3E0).copy(alpha = 0.10f + 0.04f * pulse), Color.Transparent),
                center = Offset(w * (0.5f + 0.12f * driftX), h * (0.5f + 0.1f * driftY)),
                radius = w * 0.18f,
            ),
            radius = w * 0.18f,
            center = Offset(w * (0.5f + 0.12f * driftX), h * (0.5f + 0.1f * driftY)),
        )
    }
}
