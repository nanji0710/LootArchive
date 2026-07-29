package com.nanji.lootarchive.ui.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import kotlin.math.cos
import kotlin.math.sin

data class RadarAxis(val label: String, val value: Float, val maxValue: Float)

@Composable
fun RadarChart(
    axes: List<RadarAxis>,
    modifier: Modifier = Modifier,
    fillColor: Color = Primary().copy(alpha = 0.30f),
    strokeColor: Color = Primary(),
    sizeDp: Float = 280f
) {
    if (axes.size < 3) return
    val dark = LocalDarkTheme.current
    val gridColor = if (dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
    val axisColor = if (dark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.16f)
    val labelColor = if (dark) 0xFFD6D0C8.toInt() else 0xFF44403C.toInt()

    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.width * 0.30f; val n = axes.size
        val step = (2 * Math.PI / n).toFloat()
        val labelR = r * 1.30f

        // Grid polygons (3 levels)
        for (lvl in 1..3) {
            val lr = r * lvl / 3f; val path = Path()
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                if (i == 0) path.moveTo(cx + lr * cos(a), cy + lr * sin(a))
                else path.lineTo(cx + lr * cos(a), cy + lr * sin(a))
            }
            path.close(); drawPath(path, gridColor, style = Stroke(2f))
        }

        // Axis lines
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            drawLine(axisColor, Offset(cx, cy), Offset(cx + r * cos(a), cy + r * sin(a)), strokeWidth = 1.5f)
        }

        // Data polygon
        val dp = Path()
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
            if (i == 0) dp.moveTo(cx + vr * cos(a), cy + vr * sin(a))
            else dp.lineTo(cx + vr * cos(a), cy + vr * sin(a))
        }
        dp.close(); drawPath(dp, fillColor); drawPath(dp, strokeColor, style = Stroke(3f))

        // Dots
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
            drawCircle(strokeColor, 5f, Offset(cx + vr * cos(a), cy + vr * sin(a)))
        }

        // Labels at corners
        val paint = Paint().apply {
            color = labelColor; textSize = 30f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val lx = cx + labelR * cos(a)
            val ly = cy + labelR * sin(a) + 10f
            drawContext.canvas.nativeCanvas.drawText(axes[i].label, lx, ly, paint)
        }
    }
}
