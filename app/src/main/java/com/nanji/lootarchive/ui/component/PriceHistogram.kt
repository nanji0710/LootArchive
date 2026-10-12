package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nanji.lootarchive.ui.theme.AppShape
import com.nanji.lootarchive.ui.theme.AppSpacing
import com.nanji.lootarchive.ui.theme.FredokaFont
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextSecondary

/** 客单价分布的一个档位。 */
data class HistBucket(val label: String, val count: Int)

/**
 * 客单价分布直方图（v7.2 新增）。
 *
 * 回答的是「我这堆东西是精品还是杂货」——只看总资产看不出结构，
 * 五档一拉开就知道钱是压在两三件大件上，还是散在一堆小件里。
 * 件数最集中的那一档高亮（众数），它就是你的消费档位。
 */
@Composable
fun PriceHistogram(
    buckets: List<HistBucket>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 148.dp
) {
    if (buckets.isEmpty()) return
    val textAux = TextAuxiliary()
    val textSec = TextSecondary()
    val primary = Primary()

    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)
    val modeIndex = buckets.indices.maxByOrNull { buckets[it].count } ?: 0

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
            val plotHeight = maxHeight - 20.dp // 顶部给件数行留位
            val columnWidth = maxWidth / buckets.size
            val barWidth = minOf(34.dp, columnWidth * 0.66f)

            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                buckets.forEachIndexed { i, bucket ->
                    val isMode = i == modeIndex && bucket.count > 0
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            "${bucket.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMode) primary else textAux,
                            fontWeight = if (isMode) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FredokaFont, maxLines = 1
                        )
                        Spacer(Modifier.height(AppSpacing.xs))
                        val h = if (bucket.count <= 0) 3.dp
                        else (plotHeight * (bucket.count.toFloat() / maxCount)).coerceAtLeast(4.dp)
                        Box(
                            Modifier.width(barWidth).height(h).clip(AppShape.chip)
                                .background(if (isMode) primary else primary.copy(alpha = 0.28f))
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.md)) {
            buckets.forEachIndexed { i, bucket ->
                Text(
                    bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == modeIndex) textSec else textAux,
                    fontWeight = if (i == modeIndex) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FredokaFont, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
