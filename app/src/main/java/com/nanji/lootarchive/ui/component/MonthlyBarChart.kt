package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.AppShape
import com.nanji.lootarchive.ui.theme.AppSpacing
import com.nanji.lootarchive.ui.theme.AppTypography
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.LocalGlassColors
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextSecondary
import com.nanji.lootarchive.util.FormatUtil

/** 月度购入的一根柱子。 */
data class MonthlyBar(
    /** 横轴文案，例如 `10`。 */
    val label: String,
    val value: Double,
    /** 是否当月 —— 高亮并挂数值气泡。 */
    val current: Boolean = false
)

/**
 * 月度购入柱状图（v7.2 重做）。
 *
 * 相对旧实现的改动，都是「读图时才发现的错」：
 *  - **当月高亮**：旧实现高亮的是「金额最高的月份」（StatisticsScreen.kt 旧 355 行），
 *    读起来像「历史巅峰」，但用户问的是「我最近买得凶不凶」——所以改成高亮当月。
 *  - **月均虚线**：只给绝对值看不出节奏，补一条虚线基准，回答「这个月算多还是算少」。
 *  - 数值气泡只挂在「当月」和「最高月」上：每根柱子都标数字会糊成一片。
 *
 * 传入的 [bars] 必须是**连续自然月**（空月补 0），否则柱距不等距、时间轴失真。
 */
@Composable
fun MonthlyBarChart(
    bars: List<MonthlyBar>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 134.dp
) {
    if (bars.isEmpty()) return
    val textAux = TextAuxiliary()
    val textSec = TextSecondary()
    val primary = Primary()
    val glassBg = LocalGlassColors.current.glassBg
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val maxValue = bars.maxOf { it.value }.coerceAtLeast(1.0)
    val avg = bars.sumOf { it.value } / bars.size
    val peakIndex = bars.indices.maxByOrNull { bars[it].value } ?: 0

    // 图表坐标标签：10sp 比正文梯子最小档(12sp)还小，因为它要挤在柱顶气泡与均线旁。
    // 从 AppTypography.labelSmall 派生，字族/行高与全站一致，只有字号是图表专属覆盖。
    val axisStyle = remember(textAux) {
        AppTypography.labelSmall.copy(fontFamily = FredokaFont, fontSize = 10.sp, color = textAux, fontWeight = FontWeight.Normal)
    }
    val tipStyle = remember {
        AppTypography.labelSmall.copy(fontFamily = FredokaFont, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }

    val tipReserve = 22.dp
    val gridColor = textAux.copy(alpha = 0.12f)
    val trackColor = textAux.copy(alpha = 0.07f)
    val avgColor = textAux.copy(alpha = 0.55f)

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
            val plotHeight = maxHeight - tipReserve
            val columnWidth = maxWidth / bars.size
            val barWidth = minOf(24.dp, columnWidth * 0.62f)
            val tipReservePx = with(density) { tipReserve.toPx() }

            // 基准线 + 月均虚线：只提供读数参考系，不抢图形
            Canvas(Modifier.fillMaxSize()) {
                val plotPx = size.height - tipReservePx
                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = tipReservePx + plotPx * (1f - ratio)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                if (avg > 0.0) {
                    val y = tipReservePx + plotPx * (1f - (avg / maxValue).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        avgColor, Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))
                    )
                    val label = measurer.measure("月均 ${FormatUtil.formatPriceShort(avg)}", axisStyle)
                    val lx = 2f
                    val ly = (y - label.size.height - 3f).coerceAtLeast(0f)
                    // 药丸底：虚线穿字会读不清，垫一块与卡片同色的底
                    drawRoundRect(
                        glassBg, Offset(lx - 3f, ly - 1f),
                        Size(label.size.width + 6f, label.size.height + 2f), CornerRadius(4f)
                    )
                    drawText(label, topLeft = Offset(lx, ly))
                }
            }

            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                bars.forEachIndexed { i, bar ->
                    // 只给最高的那个月挂数值：12 列里每列只有 24dp 宽，挂两个就会互相压住
                    val showTip = bar.value > 0.0 && i == peakIndex
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (showTip) {
                            Box(
                                Modifier
                                    // 首末列贴边对齐：气泡比列宽（约 24dp）宽一倍，居中会被卡片裁掉
                                    .then(
                                        when (i) {
                                            bars.lastIndex -> Modifier.align(Alignment.End)
                                            0 -> Modifier.align(Alignment.Start)
                                            else -> Modifier
                                        }
                                    )
                                    // requiredWidth 才是关键：普通 width 会被列宽约束住，
                                    // 文案折行后被 pill 裁成「¥1.」，数值直接读不出来
                                    .requiredWidth(64.dp)
                                    .clip(AppShape.pill).background(primary)
                                    .padding(horizontal = AppSpacing.xs, vertical = 1.dp)
                            ) {
                                Text(
                                    FormatUtil.formatPriceShort(bar.value),
                                    style = tipStyle, maxLines = 1, softWrap = false,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                        }
                        val h = if (bar.value <= 0.0) 3.dp
                        else (plotHeight * (bar.value / maxValue).toFloat()).coerceAtLeast(4.dp)
                        // 有数据的月份用主色渐变（浅到深向下收），空月只留一根极淡的短桩 ——
                        // 旧实现两者同色，柱子淡到读不出「哪几个月真买过东西」。
                        val brush = when {
                            bar.current -> Brush.verticalGradient(listOf(primary, primary))
                            bar.value <= 0.0 -> Brush.verticalGradient(listOf(trackColor, trackColor))
                            else -> Brush.verticalGradient(
                                listOf(primary.copy(alpha = 0.50f), primary.copy(alpha = 0.18f))
                            )
                        }
                        Box(
                            Modifier.width(barWidth).height(h).clip(AppShape.bar).background(brush)
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.md)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.current) textSec else textAux,
                    fontWeight = if (bar.current) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FredokaFont, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
