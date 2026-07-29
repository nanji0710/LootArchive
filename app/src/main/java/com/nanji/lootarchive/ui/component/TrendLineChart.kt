package com.nanji.lootarchive.ui.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import java.text.NumberFormat

data class TrendPoint(val label: String, val value: Double)

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Primary(),
    sizeDp: Float = 220f
) {
    if (points.size < 2) return
    val dark = LocalDarkTheme.current
    val gridColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val labelColor = if (dark) 0xFFB0A89E.toInt() else 0xFF55504C.toInt()
    val numberFormat = NumberFormat.getNumberInstance()

    Canvas(modifier = modifier.fillMaxWidth().height(sizeDp.dp)) {
        val w = size.width; val h = size.height
        val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
        val range = maxVal.coerceAtLeast(1.0)
        val pl = 56f; val pr = 8f; val padT = 28f; val pb = 48f
        val cw = w - pl - pr; val ch = h - padT - pb

        // Y-axis labels (3 levels)
        val labelPaint = Paint().apply { color = labelColor; textSize = 22f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        for (i in 0..2) {
            val y = padT + ch * i / 2f
            drawLine(gridColor, Offset(pl, y), Offset(w - pr, y), strokeWidth = 1.5f)
            val lbl = "¥${numberFormat.format((maxVal * (2 - i) / 2).toLong())}"
            drawContext.canvas.nativeCanvas.drawText(lbl, pl - 6f, y + 7f, labelPaint)
        }

        val stepX = cw / (points.size - 1).coerceAtLeast(1)

        // Line
        val path = Path()
        points.forEachIndexed { i, pt ->
            val x = pl + i * stepX
            val y = padT + ch - ((pt.value / range) * ch).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(3f))

        // Fill gradient
        val fp = Path(); fp.addPath(path)
        val lastX = pl + (points.size - 1) * stepX
        fp.lineTo(lastX, padT + ch); fp.lineTo(pl, padT + ch); fp.close()
        drawPath(fp, lineColor.copy(alpha = 0.12f))

        // Dots + value labels
        val valPaint = Paint().apply { color = labelColor; textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        points.forEachIndexed { i, pt ->
            val x = pl + i * stepX
            val y = padT + ch - ((pt.value / range) * ch).toFloat()
            drawCircle(lineColor, 5f, Offset(x, y))
            // Value label above dot
            drawContext.canvas.nativeCanvas.drawText("¥${numberFormat.format(pt.value.toLong())}", x, y - 10f, valPaint)
        }

        // X-axis labels (evenly spaced, max 6)
        val showEvery = ((points.size - 1) / 5).coerceAtLeast(1)
        val xLabelPaint = Paint().apply { color = labelColor; textSize = 22f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        points.forEachIndexed { i, pt ->
            if (i % showEvery == 0 || i == points.size - 1) {
                val x = pl + i * stepX
                drawContext.canvas.nativeCanvas.drawText(pt.label, x, padT + ch + 24f, xLabelPaint)
            }
        }
    }
}
