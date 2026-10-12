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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.AppTypography
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.util.FormatUtil

data class TrendPoint(val label: String, val value: Double)

/**
 * 资产净值趋势折线图 —— 横向可滚动（对齐月度购入趋势模块）。
 * 每月份固定列宽（默认 52dp），初始定位到最新月份。
 *
 * v7.1 视觉升级：三条极淡的水平基准线 + 渐变面积（顶部浓、底部透）
 * + 端点「药丸徽标」直接标出最新累计额，中间点缩小并淡出，
 * 让视线自然落到最新数值上（参考图里折线终点带数值气泡的做法）。
 */
@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    sizeDp: Float = 200f,
    columnWidthDp: Dp = 52.dp
) {
    if (points.size < 2) return
    val textAux = TextAuxiliary()
    val primary = Primary()
    val gridColor = textAux.copy(alpha = 0.14f)
    val measurer = rememberTextMeasurer()
    val yStyle = remember(textAux) {
        // 图表坐标标签：10sp 比正文梯子最小档(12sp)还小，因为它要挤在
        // 折线图的点旁边与横轴下方。从 AppTypography.labelSmall 派生，
        // 字族/行高与全站一致，只有字号是局部（图表专属）覆盖。
        AppTypography.labelSmall.copy(
            fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal
        )
    }
    val xStyle = remember(textAux) {
        AppTypography.labelSmall.copy(
            fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal
        )
    }
    // 端点徽标：白字压在主题色药丸上，字重加粗一档
    val badgeStyle = remember {
        AppTypography.labelSmall.copy(
            fontFamily = FredokaFont, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold
        )
    }

    val scrollState = rememberScrollState()
    // 布局完成后滚动到最右：初始显示最新月份
    LaunchedEffect(scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
    val range = maxVal.coerceAtLeast(1.0)
    val padT = 46f; val pb = 26f

    Row(
        modifier = modifier.fillMaxWidth().height(sizeDp.dp).horizontalScroll(scrollState),
        verticalAlignment = Alignment.Bottom
    ) {
        Canvas(Modifier.width(columnWidthDp * points.size).fillMaxHeight()) {
            val w = size.width; val h = size.height
            val ch = h - padT - pb
            val colW = w / points.size
            val baseY = padT + ch

            // 基准线：0 / 50% / 100% 三条，极淡 —— 只提供"读数参考系"，不抢图形
            listOf(0f, 0.5f, 1f).forEach { ratio ->
                val y = baseY - ch * ratio
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            val path = Path()
            points.forEachIndexed { i, pt ->
                val x = colW * i + colW / 2f; val y = baseY - ((pt.value / range) * ch).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // 渐变面积：贴线处最浓，落到底线完全透明
            val fp = Path(); fp.addPath(path)
            fp.lineTo(colW * (points.size - 1) + colW / 2f, baseY)
            fp.lineTo(colW / 2f, baseY); fp.close()
            drawPath(
                fp,
                Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.30f), primary.copy(alpha = 0.02f)),
                    startY = 0f,
                    endY = baseY
                )
            )
            drawPath(path, primary, style = Stroke(3f))

            points.forEachIndexed { i, pt ->
                val x = colW * i + colW / 2f; val y = baseY - ((pt.value / range) * ch).toFloat()
                val isLast = i == points.lastIndex
                if (isLast) {
                    // 端点：白圈衬托 + 实心点 + 数值药丸
                    drawCircle(Color.White.copy(alpha = 0.9f), 8f, Offset(x, y))
                    drawCircle(primary, 5.5f, Offset(x, y))
                    val bl = measurer.measure(FormatUtil.formatPriceFull(pt.value), badgeStyle)
                    val bw = bl.size.width + 18f
                    val bh = bl.size.height + 10f
                    val bx = (x - bw / 2f).coerceIn(0f, (w - bw).coerceAtLeast(0f))
                    val by = (y - 16f - bh).coerceIn(0f, (h - bh).coerceAtLeast(0f))
                    drawRoundRect(primary, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2f))
                    drawText(bl, topLeft = Offset(bx + 9f, by + 5f))
                } else {
                    drawCircle(primary.copy(alpha = 0.85f), 3.5f, Offset(x, y))
                    val vl = measurer.measure(FormatUtil.formatPriceShort(pt.value), yStyle)
                    drawText(
                        vl,
                        topLeft = Offset(
                            (x - vl.size.width / 2f).coerceIn(0f, (w - vl.size.width).coerceAtLeast(0f)),
                            (y - 14f - vl.size.height).coerceAtLeast(0f)
                        )
                    )
                }
                val xl = measurer.measure(pt.label, xStyle)
                drawText(
                    xl,
                    topLeft = Offset(
                        (x - xl.size.width / 2f).coerceIn(0f, (w - xl.size.width).coerceAtLeast(0f)),
                        baseY + 8f
                    )
                )
            }
        }
    }
}
