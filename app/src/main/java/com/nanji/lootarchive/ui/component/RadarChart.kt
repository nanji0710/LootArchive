package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.nanji.lootarchive.ui.theme.FredokaFont
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
    val textAux = TextAuxiliary()
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

    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.width * 0.30f; val n = axes.size
        val step = (2 * Math.PI / n).toFloat()
        val labelR = r * 1.30f

        for (lvl in 1..3) {
            val lr = r * lvl / 3f; val path = Path()
            for (i in 0 until n) {
                val a = -Math.PI.toFloat() / 2f + i * step
                if (i == 0) path.moveTo(cx + lr * cos(a), cy + lr * sin(a))
                else path.lineTo(cx + lr * cos(a), cy + lr * sin(a))
            }
            path.close(); drawPath(path, gridColor, style = Stroke(2f))
        }
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            drawLine(axisColor, Offset(cx, cy), Offset(cx + r * cos(a), cy + r * sin(a)), strokeWidth = 1.5f)
        }
        val dp = Path()
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
            if (i == 0) dp.moveTo(cx + vr * cos(a), cy + vr * sin(a))
            else dp.lineTo(cx + vr * cos(a), cy + vr * sin(a))
        }
        dp.close(); drawPath(dp, fillColor); drawPath(dp, strokeColor, style = Stroke(3f))
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val vr = r * (axes[i].value / axes[i].maxValue).coerceIn(0f, 1f)
            drawCircle(strokeColor, 5f, Offset(cx + vr * cos(a), cy + vr * sin(a)))
        }
        // Labels at corners using Compose TextMeasurer
        for (i in 0 until n) {
            val a = -Math.PI.toFloat() / 2f + i * step
            val textLayout = measurer.measure(axes[i].label, labelStyle)
            val lx = cx + labelR * cos(a) - textLayout.size.width / 2f
            val ly = cy + labelR * sin(a) - textLayout.size.height / 2f
            drawText(textLayout, topLeft = Offset(lx, ly))
        }
    }
}
