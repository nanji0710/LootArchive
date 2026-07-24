package com.nanji.lootarchive.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

// ─── 传递 APP 主题模式和主色给所有子组件 ───
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalPrimaryColor = staticCompositionLocalOf { Color(0xFFD4A574) }

internal val LightColorScheme: ColorScheme
    @Composable
    get() {
        val p = LocalPrimaryColor.current
        return lightColorScheme(
            primary = p, onPrimary = Color.White,
            secondary = _TextSecondaryLight, background = _BackgroundLight, surface = _SurfaceLight,
            surfaceVariant = _GlassBgLight,
            onBackground = _TextPrimaryLight, onSurface = _TextPrimaryLight, onSurfaceVariant = _TextSecondaryLight,
            outline = _GlassBorderLight
        )
    }

internal val DarkColorScheme: ColorScheme
    @Composable
    get() {
        val p = LocalPrimaryColor.current
        // 深色模式下提亮主色
        val bright = brighten(p)
        return darkColorScheme(
            primary = bright, onPrimary = Color(0xFF1A1A1A),
            secondary = _TextSecondaryDark, background = _BackgroundDark, surface = _SurfaceDark,
            surfaceVariant = _GlassBgDark,
            onBackground = _TextPrimaryDark, onSurface = _TextPrimaryDark, onSurfaceVariant = _TextSecondaryDark,
            outline = _GlassBorderDark
        )
    }

private fun brighten(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.7f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 1.35f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun LootArchiveTheme(themeMode: String = "system", primaryColor: Int = 0xFFD4A574.toInt(), content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "dark" -> true; "light" -> false; else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController((view.context as Activity).window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalPrimaryColor provides Color(primaryColor)
    ) {
        val cs = if (darkTheme) DarkColorScheme else LightColorScheme
        MaterialTheme(colorScheme = cs, typography = AppTypography, content = content)
    }
}
