package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.AppTypography
import com.nanji.lootarchive.ui.theme.CardBg
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 雷达图单轴。
 *
 * v7.1 起 [valueLabel] 可带上金额文案：轴名下方再画一行数值（如 ¥26,119），
 * 让「分类多维对比」不用对着图猜金额 —— 参考图里每个轴都是「名称 + 金额」两行。
 */
data class RadarAxis(
    val label: String,
    val value: Float,
    val maxValue: Float,
    val valueLabel: String = ""
)

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
    val gridColor = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
    val axisColor = if (dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
    val textAux = TextAuxiliary()
    val valueColor = TextPrimary()
    val dotHalo = CardBg()
    val measurer = rememberTextMeasurer()
    val labelStyle = remember(textAux) {
        // 图表轴标签：11sp 比正文梯子的最小档(12sp)还小一档，因为要贴在
        // 240dp 见方的雷达图边缘、且必须让位于图形本身。直接从 AppTypography
        // 的 labelSmall 派生（而不是读 MaterialTheme.typography），
        // 好处是字族/行高跟全站一致、只有字号是局部覆盖，同时不引入
        // 组合局部读取，remember 的键仍然只需要 textAux。
        AppTypography.labelSmall.copy(
            fontFamily = FredokaFont,
            fontSize = 11.sp,
            color = textAux,
            fontWeight = FontWeight.Medium
        )
    }
    // 轴金额：同一档字号，但用正文色 + SemiBold，跟灰色轴名拉开层次
    val valueStyle = remember(valueColor) {
        AppTypography.labelSmall.copy(
            fontFamily = FredokaFont,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }

    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.width * 0.30f; val n = axes.size
        val step = (2 * Math.PI / n).toFloat()
        val labelR = r * 1.26f

        // 网格：3 圈等距多边形，最外圈略深一点（虚拟的"满分线"）
        for (lvl in 1..3) {
            val lr = r * lvl / 3f; val path = Path()
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                if (i == 0) path.moveTo(cx + lr * cos(a), cy + lr * sin(a))
                else path.lineTo(cx + lr * cos(a), cy + lr * sin(a))
            }
            path.close()
            drawPath(path, if (lvl == 3) gridColor.copy(alpha = gridColor.alpha * 1.4f) else gridColor, style = Stroke(2f))
        }
        // 轴线：到最外圈为止
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            drawLine(axisColor, Offset(cx, cy), Offset(cx + r * cos(a), cy + r * sin(a)), strokeWidth = 1.5f)
        }
        // 数据多边形：中心亮、边缘淡的径向渐变，比纯色块更有体积感
        val dp = Path()
        val vertices = ArrayList<Offset>(n)
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
            val p = Offset(cx + vr * cos(a), cy + vr * sin(a))
            vertices.add(p)
            if (i == 0) dp.moveTo(p.x, p.y) else dp.lineTo(p.x, p.y)
        }
        dp.close()
        drawPath(
            dp,
            Brush.radialGradient(
                colors = listOf(strokeColor.copy(alpha = 0.42f), strokeColor.copy(alpha = 0.10f)),
                center = Offset(cx, cy),
                radius = r
            )
        )
        drawPath(dp, strokeColor, style = Stroke(3f))
        // 顶点：底色描白圈 + 彩色实心点，压在网格线上也看得清
        vertices.forEach { p ->
            drawCircle(dotHalo, 6.5f, p)
            drawCircle(strokeColor, 4f, p)
        }
        // 轴标签：名称一行，金额一行；水平方向限制在画布内，避免贴边被裁
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val labelLayout = measurer.measure(axes[i].label, labelStyle)
            val valueLayout = if (axes[i].valueLabel.isBlank()) null else measurer.measure(axes[i].valueLabel, valueStyle)
            val blockH = labelLayout.size.height + (valueLayout?.size?.height ?: 0)
            val anchorX = cx + labelR * cos(a)
            val anchorY = cy + labelR * sin(a)
            val lx = (anchorX - labelLayout.size.width / 2f).coerceIn(0f, (size.width - labelLayout.size.width).coerceAtLeast(0f))
            val ly = (anchorY - blockH / 2f).coerceIn(0f, (size.height - blockH).coerceAtLeast(0f))
            drawText(labelLayout, topLeft = Offset(lx, ly))
            valueLayout?.let {
                val vx = (anchorX - it.size.width / 2f).coerceIn(0f, (size.width - it.size.width).coerceAtLeast(0f))
                drawText(it, topLeft = Offset(vx, ly + labelLayout.size.height))
            }
        }
    }
}
