package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.FredokaFont
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
    sizeDp: Float = 240f
) {
    if (points.size < 2) return
    val dark = LocalDarkTheme.current
    val gridColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val textAux = TextAuxiliary()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    val measurer = rememberTextMeasurer()
    val valStyle = remember(textAux) { TextStyle(fontFamily = FredokaFont, fontSize = 8.sp, color = textAux, fontWeight = FontWeight.Normal) }
    val yStyle = remember(textAux) { TextStyle(fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal) }
    val xStyle = remember(textAux) { TextStyle(fontFamily = FredokaFont, fontSize = 11.sp, color = textAux, fontWeight = FontWeight.Normal) }

    Canvas(modifier = modifier.fillMaxWidth().height(sizeDp.dp)) {
        val w = size.width; val h = size.height
        val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
        val range = maxVal.coerceAtLeast(1.0)
        val pl = 52f; val pr = 8f; val padT = 34f; val pb = 46f
        val ch = h - padT - pb

        // Y-axis grid + labels
        for (i in 0..2) {
            val y = padT + ch * i / 2f
            drawLine(gridColor, Offset(pl, y), Offset(w - pr, y), strokeWidth = 1.5f)
            val yl = measurer.measure("¥${numberFormat.format((maxVal * (2 - i) / 2).toLong())}", yStyle)
            drawText(yl, topLeft = Offset(pl - yl.size.width - 4f, y - yl.size.height / 2f))
        }

        val cw = w - pl - pr; val stepX = cw / (points.size - 1).coerceAtLeast(1)

        // Line
        val path = Path()
        points.forEachIndexed { i, pt ->
            val x = pl + i * stepX; val y = padT + ch - ((pt.value / range) * ch).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(3f))
        val fp = Path(); fp.addPath(path)
        val lastX = pl + (points.size - 1) * stepX
        fp.lineTo(lastX, padT + ch); fp.lineTo(pl, padT + ch); fp.close()
        drawPath(fp, lineColor.copy(alpha = 0.12f))

        // Dots + value labels
        points.forEachIndexed { i, pt ->
            val x = pl + i * stepX; val y = padT + ch - ((pt.value / range) * ch).toFloat()
            drawCircle(lineColor, 5f, Offset(x, y))
            val vl = measurer.measure("¥${numberFormat.format(pt.value.toLong())}", valStyle)
            drawText(vl, topLeft = Offset(x - vl.size.width / 2f, y - 14f - vl.size.height))
        }

        // X-axis labels
        val showEvery = ((points.size - 1) / 5).coerceAtLeast(1)
        points.forEachIndexed { i, pt ->
            if (i % showEvery == 0 || i == points.size - 1) {
                val xl = measurer.measure(pt.label, xStyle)
                drawText(xl, topLeft = Offset(pl + i * stepX - xl.size.width / 2f, padT + ch + 10f))
            }
        }
    }
}
