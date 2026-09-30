package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  顶部渐隐 —— 页面骨架的统一件
//
//  为什么需要它：三个主页面都是"内容从透明状态栏底下滑过去"的沉浸式布局。
//  没有渐隐时，列表第一项被视口**硬切**一刀 —— 文字上半截突然消失，
//  看着像渲染坏了。加上渐隐，内容是被"化掉"的。这是 jicun（即存）那边
//  用 ShaderMask + BlendMode.dstIn 做的同一件事，这里用 Compose 的等价做法。
//
//  两个必须记住的点：
//
//  1. **CompositingStrategy.Offscreen 是必需的**。BlendMode.DstIn 只作用于
//     当前图层；不隔离成独立图层的话，它会往上作用到父图层，把底栏、
//     背景一起擦掉（一整屏变黑）。这是这个技巧最容易踩的坑。
//
//  2. **遮罩始终挂载**，不在"没滚动"时换成不加遮罩的分支。换分支会改变
//     该位置的 Modifier 结构，导致整棵滚动容器重建 —— 滚动位置、展开态
//     全丢。所以未滚动时返回的是一块**全不透明**遮罩，等效于没有遮罩。
//
//  带宽跟着滚动量长：停在顶部时第一项是**完整**的（不该一进页面就淡掉半行），
//  往下滑才逐渐加深，到 [ScrollFadeHeight] 封顶。
//
//  读取滚动量发生在**绘制阶段**（drawWithContent 内部），所以滚动时只会
//  重画、不会重组调用方 —— 这正是想要的。
// ═══════════════════════════════════════════════════════════════

/** 渐隐带满宽。28dp 取自 jicun 的实测值：再宽会吃掉第一张卡的内容。 */
val ScrollFadeHeight: Dp = 28.dp

/** `ScrollState`（普通 verticalScroll）。 */
fun Modifier.topFade(
    scrollState: ScrollState,
    fadeHeight: Dp = ScrollFadeHeight,
): Modifier = topFadeImpl(fadeHeight) { scrollState.value.toFloat() }

/** `LazyListState`（LazyColumn）。 */
fun Modifier.topFade(
    listState: LazyListState,
    fadeHeight: Dp = ScrollFadeHeight,
): Modifier = topFadeImpl(fadeHeight) {
    // 第一项还在屏幕上时用它的像素偏移；已经滚过第一项就直接给满带宽。
    if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
    else listState.firstVisibleItemScrollOffset.toFloat()
}

/** `LazyGridState`（LazyVerticalGrid）。 */
fun Modifier.topFade(
    gridState: LazyGridState,
    fadeHeight: Dp = ScrollFadeHeight,
): Modifier = topFadeImpl(fadeHeight) {
    if (gridState.firstVisibleItemIndex > 0) Float.MAX_VALUE
    else gridState.firstVisibleItemScrollOffset.toFloat()
}

private fun Modifier.topFadeImpl(
    fadeHeight: Dp,
    scrollOffsetPx: () -> Float,
): Modifier = this
    // Offscreen：把内容先画进独立图层，DstIn 才只作用于这一层（见文件头说明 1）
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val bandPx = fadeHeight.toPx()
        val band = scrollOffsetPx().coerceIn(0f, bandPx).coerceAtMost(bandPx)
        // 没滚动（或带宽算出来是 0）就什么都不做 —— 注意这里**不是**返回
        // 另一个 Modifier，遮罩结构本身没有变化，只是不画。
        if (band > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = band,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

/**
 * "这一页是否已经滚动"的信号。需要按滚动状态做视觉变化的调用方用它 ——
 * 例如首页的搜索栏/FAB 在滚动时才隐藏。
 *
 * 用 `derivedStateOf` 是为了只在**跨过阈值**时触发重组，
 * 而不是每滚一个像素就重组一次调用方。
 */
@Composable
fun rememberIsScrolled(scrollState: ScrollState): State<Boolean> =
    remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

@Composable
fun rememberIsScrolled(listState: LazyListState): State<Boolean> =
    remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
