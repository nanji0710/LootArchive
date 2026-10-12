package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class TrendPoint(val label: String, val value: Double)

/** 把任意量程向上取整到 1/2/5×10ⁿ —— 刻度只出「好看数」，不出 ¥9.66K 这种数。 */
private fun niceStep(raw: Double): Double {
    if (raw <= 0.0 || !raw.isFinite()) return 1.0
    val base = 10.0.pow(floor(log10(raw)))
    val n = raw / base
    val mult = when {
        n <= 1.0 -> 1.0
        n <= 2.0 -> 2.0
        n <= 5.0 -> 5.0
        else -> 10.0
    }
    return mult * base
}

/**
 * 资产净值趋势折线图（v7.2 重做）。
 *
 * 旧实现是「每月 52dp 固定列宽 + 横向滚动」，屏幕上只能看到最新 6 个月左右——
 * 卡片头写着「37 个月」，图里却只有 5 个点，两句话互相打脸；而且横向滚动条藏在
 * 纵向滚动页里，用户根本不会去拖。现在**整段历史铺满卡片宽度**，一眼看完走势。
 *
 * 随之而来的两个必改项（都是旧实现留下的读图错误）：
 *  - 旧实现给**每个点**都标金额，点一密集就叠成 `¥9.66K¥7.97K`；现在只在终点留
 *    药丸徽标（累计曲线单调递增，终点就是最大值，中间点标了也只是噪声）。
 *  - 旧实现横轴刻度按 max/3 取，出现 `¥9.66K`；现在按 1/2/5×10ⁿ 取整步长，
 *    刻度是 ¥0 / ¥1.0万 / ¥2.0万 / ¥3.0万。
 */
@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    sizeDp: Float = 200f,
    yAxisWidth: Dp = 46.dp
) {
    if (points.size < 2) return
    val textAux = TextAuxiliary()
    val primary = Primary()
    val gridColor = textAux.copy(alpha = 0.14f)
    val measurer = rememberTextMeasurer()

    // 图表坐标标签：10sp 比正文梯子最小档(12sp)还小，因为它要挤在折线图的点旁边与横轴下方。
    // 从 AppTypography.labelSmall 派生，字族/行高与全站一致，只有字号是图表专属覆盖。
    val axisStyle = remember(textAux) {
        AppTypography.labelSmall.copy(fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal)
    }
    // 端点徽标：白字压在主题色药丸上，字重加粗一档
    val badgeStyle = remember {
        AppTypography.labelSmall.copy(fontFamily = FredokaFont, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }

    val peak = points.maxOf { it.value }
    val step = remember(peak) { niceStep(peak / 3.0) }
    val axisTop = step * 3.0

    // 横轴文案最多放 5 个，多出来的月份只留点、不留字
    val labelEvery = remember(points.size) { ceil(points.size / 5.0).toInt().coerceAtLeast(1) }

    Row(modifier.fillMaxWidth().height(sizeDp.dp)) {
        // 左侧金额刻度：不随内容滚动。曲线处在哪个量级，靠这四条读数参考系。
        Canvas(Modifier.width(yAxisWidth).fillMaxHeight()) {
            val padT = 30.dp.toPx()
            val padB = 22.dp.toPx()
            val ch = size.height - padT - padB
            listOf(3, 2, 1, 0).forEach { k ->
                val y = padT + ch * (1f - k / 3f)
                val l = measurer.measure(FormatUtil.formatPriceShort(step * k), axisStyle)
                drawText(
                    l,
                    topLeft = Offset(
                        (size.width - l.size.width - 2f).coerceAtLeast(0f),
                        (y - l.size.height / 2f).coerceIn(0f, (size.height - l.size.height).coerceAtLeast(0f))
                    )
                )
            }
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            val padT = 30.dp.toPx()
            val padB = 22.dp.toPx()
            val ch = h - padT - padB
            val baseY = padT + ch
            val n = points.size
            val colW = w / n
            val xOf = { i: Int -> colW * (i + 0.5f) }
            val yOf = { v: Double -> baseY - (v / axisTop * ch).toFloat() }

            // 基准线：0 / 1/3 / 2/3 / 满量程 四条，与左侧刻度一一对齐 —— 只提供参考系，不抢图形
            listOf(0f, 1f / 3f, 2f / 3f, 1f).forEach { ratio ->
                val y = baseY - ch * ratio
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            val path = Path()
            points.forEachIndexed { i, pt ->
                val x = xOf(i); val y = yOf(pt.value)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // 渐变面积：贴线处最浓，落到底线完全透明
            val fp = Path(); fp.addPath(path)
            fp.lineTo(xOf(n - 1), baseY); fp.lineTo(xOf(0), baseY); fp.close()
            drawPath(
                fp,
                Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.30f), primary.copy(alpha = 0.02f)),
                    startY = 0f,
                    endY = baseY
                )
            )
            drawPath(path, primary, style = Stroke(3f))

            // 横轴文案：等间隔抽稀 + 与上一个标签撞车就跳过，末尾月份优先保留
            var lastRight = -1e9f
            points.forEachIndexed { i, pt ->
                if (i % labelEvery != 0 && i != n - 1) return@forEachIndexed
                val l = measurer.measure(pt.label, axisStyle)
                val lx = (xOf(i) - l.size.width / 2f).coerceIn(0f, (w - l.size.width).coerceAtLeast(0f))
                if (lx < lastRight + 4f) return@forEachIndexed
                drawText(l, topLeft = Offset(lx, baseY + 8f))
                lastRight = lx + l.size.width
            }

            // 终点：白圈衬托 + 实心点 + 数值药丸。累计曲线单调递增，终点即最大值。
            val ex = xOf(n - 1); val ey = yOf(points.last().value)
            drawCircle(Color.White.copy(alpha = 0.9f), 8f, Offset(ex, ey))
            drawCircle(primary, 5.5f, Offset(ex, ey))
            val bl = measurer.measure(FormatUtil.formatPriceFull(points.last().value), badgeStyle)
            val bw = bl.size.width + 18f
            val bh = bl.size.height + 10f
            val bx = (ex - bw / 2f).coerceIn(0f, (w - bw).coerceAtLeast(0f))
            val by = (ey - 12.dp.toPx() - bh).coerceAtLeast(0f)
            drawRoundRect(primary, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2f))
            drawText(bl, topLeft = Offset(bx + 9f, by + 5f))
        }
    }
}
