package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.Backdrop
import com.nanji.lootarchive.ui.theme.*

/**
 * 页面级捕获层（Backdrop），供悬浮玻璃组件（底栏/搜索栏/FAB/弹窗）采样。
 * 页面内组件不直接采样本层（会自采样），需用局部捕获层或半透明实体。
 */
val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun ProvidePageBackdrop(backdrop: Backdrop, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPageBackdrop provides backdrop, content = content)
}

/** 暖色垂直渐变背景 —— 玻璃有内容可糊的画布 */
@Composable
fun backgroundBrush(): Brush {
    val dark = LocalDarkTheme.current
    return Brush.verticalGradient(
        if (dark) listOf(_BackgroundGradientDarkStart, _BackgroundGradientDarkEnd)
        else listOf(_BackgroundGradientLightStart, _BackgroundGradientLightEnd)
    )
}
