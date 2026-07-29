package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import kotlin.math.cos
import kotlin.math.sin

data class RadarAxis(val label: String, val value: Float, val maxValue: Float)

@Composable
fun RadarChart(
    axes: List<RadarAxis>,
    modifier: Modifier = Modifier,
    fillColor: Color = Primary().copy(alpha = 0.15f),
    strokeColor: Color = Primary(),
    sizeDp: Float = 260f
) {
    if (axes.size < 2) return
    val gridColor = TextAuxiliary().copy(alpha = 0.12f)
    val axisColor = TextAuxiliary().copy(alpha = 0.18f)
    val labelColor = TextAuxiliary()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(sizeDp.dp)) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val r = size.width * 0.35f; val n = axes.size
            val step = (2 * Math.PI / n).toFloat()

            // Grid
            for (lvl in 1..3) {
                val lr = r * lvl / 3f; val path = Path()
                for (i in 0 until n) {
                    val a = -Math.PI.toFloat() / 2f + i * step
                    if (i == 0) path.moveTo(cx + lr * cos(a), cy + lr * sin(a))
                    else path.lineTo(cx + lr * cos(a), cy + lr * sin(a))
                }
                path.close(); drawPath(path, gridColor, style = Stroke(1.5f))
            }

            // Axis lines
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                drawLine(axisColor, Offset(cx, cy), Offset(cx + r * cos(a), cy + r * sin(a)), strokeWidth = 1f)
            }

            // Data polygon
            val dp = Path()
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
                if (i == 0) dp.moveTo(cx + vr * cos(a), cy + vr * sin(a))
                else dp.lineTo(cx + vr * cos(a), cy + vr * sin(a))
            }
            dp.close(); drawPath(dp, fillColor); drawPath(dp, strokeColor, style = Stroke(2.5f))

            // Dots
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
                drawCircle(strokeColor, 5f, Offset(cx + vr * cos(a), cy + vr * sin(a)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            axes.forEach { ax -> Text(ax.label, fontSize = 10.sp, color = labelColor, fontFamily = FredokaFont) }
        }
    }
}
