package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.util.FormatUtil

data class TrendPoint(val label: String, val value: Double)

/**
 * 资产净值趋势折线图 —— 横向可滚动（对齐月度购入趋势模块）。
 * 每月份固定列宽（默认 52dp），初始定位到最新月份；左轴固定不滚动；金额短格式防重叠。
 */
@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    sizeDp: Float = 200f,
    columnWidthDp: Dp = 52.dp
) {
    if (points.size < 2) return
    val dark = LocalDarkTheme.current
    val gridColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val textAux = TextAuxiliary()
    val primary = Primary()
    val measurer = rememberTextMeasurer()
    val yStyle = remember(textAux) { TextStyle(fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal) }
    val xStyle = remember(textAux) { TextStyle(fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal) }

    val scrollState = rememberScrollState()
    // 布局完成后滚动到最右：初始显示最新月份
    LaunchedEffect(scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
    val range = maxVal.coerceAtLeast(1.0)
    val yAxisWidth = 64.dp
    val padT = 40f; val pb = 24f

    Row(
        modifier = modifier.height(sizeDp.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 固定左轴标签（不随横向滚动），右对齐、短金额格式
        Canvas(Modifier.width(yAxisWidth).fillMaxHeight()) {
            for (i in 0..2) {
                val y = padT + (size.height - padT - pb) * i / 2f
                val yl = measurer.measure("¥${FormatUtil.formatPriceShort(maxVal * (2 - i) / 2)}", yStyle)
                drawText(yl, topLeft = Offset(size.width - yl.size.width, y - yl.size.height / 2f))
            }
        }
        // 横向滚动图表区
        Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(scrollState)) {
            Canvas(Modifier.width(columnWidthDp * points.size).fillMaxHeight()) {
                val w = size.width; val h = size.height
                val ch = h - padT - pb
                val colW = w / points.size
                for (i in 0..2) {
                    val y = padT + ch * i / 2f
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }
                val path = Path()
                points.forEachIndexed { i, pt ->
                    val x = colW * i + colW / 2f; val y = padT + ch - ((pt.value / range) * ch).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, primary, style = Stroke(3f))
                val fp = Path(); fp.addPath(path)
                fp.lineTo(colW * (points.size - 1) + colW / 2f, padT + ch)
                fp.lineTo(colW / 2f, padT + ch); fp.close()
                drawPath(fp, primary.copy(alpha = 0.12f))
                points.forEachIndexed { i, pt ->
                    val x = colW * i + colW / 2f; val y = padT + ch - ((pt.value / range) * ch).toFloat()
                    drawCircle(primary, 5f, Offset(x, y))
                    val vl = measurer.measure(FormatUtil.formatPriceShort(pt.value), yStyle)
                    drawText(
                        vl,
                        topLeft = Offset(
                            (x - vl.size.width / 2f).coerceIn(0f, (w - vl.size.width).coerceAtLeast(0f)),
                            (y - 16f - vl.size.height).coerceAtLeast(0f)
                        )
                    )
                    val xl = measurer.measure(pt.label, xStyle)
                    drawText(
                        xl,
                        topLeft = Offset(
                            (x - xl.size.width / 2f).coerceIn(0f, (w - xl.size.width).coerceAtLeast(0f)),
                            padT + ch + 8f
                        )
                    )
                }
            }
        }
    }
}