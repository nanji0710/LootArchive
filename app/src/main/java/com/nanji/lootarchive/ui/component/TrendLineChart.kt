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

data class TrendPoint(val label: String, val value: Double)

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Primary(),
    sizeDp: Float = 200f
) {
    if (points.size < 2) return
    val gridColor = TextAuxiliary().copy(alpha = 0.10f)
    val labelColor = TextAuxiliary()
    val fillColor = lineColor.copy(alpha = 0.08f)
    Column(modifier = modifier) {
        Canvas(Modifier.fillMaxWidth().height(sizeDp.dp)) {
            val w = size.width; val h = size.height
            val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
            val minVal = points.minOf { it.value }.coerceAtLeast(0.0)
            val range = (maxVal - minVal).coerceAtLeast(1.0)
            val pl = 50f; val pr = 12f; val padT = 16f; val pb = 28f
            val cw = w - pl - pr; val ch = h - padT - pb

            // Grid lines
            for (i in 0..3) {
                val y = padT + ch * i / 3f
                drawLine(gridColor, Offset(pl, y), Offset(w - pr, y), strokeWidth = 1f)
            }

            val stepX = cw / (points.size - 1).coerceAtLeast(1)
            // Line
            val path = Path()
            points.forEachIndexed { i, p ->
                val x = pl + i * stepX; val y = padT + ch - ((p.value - minVal) / range * ch).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(2.5f))

            // Fill
            val fp = Path(); fp.addPath(path)
            val lastX = pl + (points.size - 1) * stepX
            fp.lineTo(lastX, padT + ch); fp.lineTo(pl, padT + ch); fp.close()
            drawPath(fp, fillColor)

            // Dots
            points.forEachIndexed { i, p ->
                val x = pl + i * stepX
                val y = padT + ch - ((p.value - minVal) / range * ch).toFloat()
                drawCircle(lineColor, 4f, Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 50.dp, end = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { pt -> Text(pt.label.takeLast(5), fontSize = 10.sp, color = labelColor, fontFamily = FredokaFont) }
        }
    }
}
