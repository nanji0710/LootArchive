package com.nanji.lootarchive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 玻璃效果 Modifier 配置
 *
 * 通过半透明背景 + 阴影 + 细边框模拟毛玻璃风格
 * 在主页面和核心页面使用
 * 注：真实背景模糊（frosted glass）需要 RenderEffect（API 31+）或额外库（如 haze），
 * 当前采用半透明方案具有良好的兼容性和性能。
 */
object GlassConfig {
    // 透明度
    const val ALPHA_LIGHT = 0.88f
    const val ALPHA_DARK = 0.80f

    // 阴影
    val SHADOW_ELEVATION = 4.dp
    val SHADOW_RADIUS = 8.dp
}

/**
 * 获取当前模式下的玻璃背景色
 */
@Composable
fun glassBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        GlassBackgroundDark
    } else {
        GlassBackgroundLight
    }
}

/**
 * 获取当前模式下的玻璃边框色
 */
@Composable
fun glassBorderColor(): Color {
    return if (isSystemInDarkTheme()) {
        GlassBorderDark
    } else {
        GlassBorderLight
    }
}

/**
 * 应用玻璃效果的 Modifier
 *
 * @param shape 组件形状
 * @param withShadow 是否添加阴影
 * @param withBorder 是否添加边框
 */
@Composable
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(12.dp),
    withShadow: Boolean = true,
    withBorder: Boolean = true
): Modifier {
    val bgColor = glassBackgroundColor()
    val borderColor = glassBorderColor()

    return this
        .then(
            if (withShadow) {
                Modifier.shadow(
                    elevation = GlassConfig.SHADOW_ELEVATION,
                    shape = shape,
                    ambientColor = bgColor.copy(alpha = 0.2f),
                    spotColor = bgColor.copy(alpha = 0.25f)
                )
            } else Modifier
        )
        .clip(shape)
        .background(bgColor)
        .then(
            if (withBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                )
            } else Modifier
        )
}
