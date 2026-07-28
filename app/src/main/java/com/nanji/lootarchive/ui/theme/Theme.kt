package com.nanji.lootarchive.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

// ── 传递 APP 主题模式和主色 ──
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalPrimaryColor = staticCompositionLocalOf { Color(0xFFFF7A3D) }

// ══════════════════════════════════════════════════════════════
//  Light ColorScheme — 现代拟物风 · 冷灰底 + 光影层次
// ══════════════════════════════════════════════════════════════
internal val LightColorScheme: ColorScheme
    @Composable
    get() {
        val p = LocalPrimaryColor.current
        return lightColorScheme(
            primary = p,
            onPrimary = Color.White,
            primaryContainer = p.copy(alpha = 0.12f),
            onPrimaryContainer = Color(0xFF3D1A00),
            secondary = _Accent,
            onSecondary = Color.White,
            secondaryContainer = _Accent.copy(alpha = 0.12f),
            onSecondaryContainer = Color(0xFF001540),
            background = _BackgroundLight,
            onBackground = _TextPrimaryLight,
            surface = _SurfaceLight,
            onSurface = _TextPrimaryLight,
            surfaceVariant = _CardLight,
            onSurfaceVariant = _TextSecondaryLight,
            outline = _TextAuxiliaryLight.copy(alpha = 0.25f),
            outlineVariant = _TextAuxiliaryLight.copy(alpha = 0.12f),
            error = WarrantyExpired,
            onError = Color.White,
            errorContainer = WarrantyExpired.copy(alpha = 0.12f),
            onErrorContainer = Color(0xFF410002),
            inverseSurface = Color(0xFF2D3748),
            inverseOnSurface = Color(0xFFEDF2F7),
            inversePrimary = _PrimaryDark,
        )
    }

// ══════════════════════════════════════════════════════════════
//  Dark ColorScheme — 深灰蓝黑底 + 提亮主色
// ══════════════════════════════════════════════════════════════
internal val DarkColorScheme: ColorScheme
    @Composable
    get() {
        val p = LocalPrimaryColor.current
        val bright = brighten(p)
        return darkColorScheme(
            primary = bright,
            onPrimary = Color(0xFF2D1500),
            primaryContainer = bright.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFFFFDCC0),
            secondary = _Accent,
            onSecondary = Color(0xFF001540),
            secondaryContainer = _Accent.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFFB0D0FF),
            background = _BackgroundDark,
            onBackground = _TextPrimaryDark,
            surface = _SurfaceDark,
            onSurface = _TextPrimaryDark,
            surfaceVariant = _CardDark,
            onSurfaceVariant = _TextSecondaryDark,
            outline = _TextAuxiliaryDark.copy(alpha = 0.25f),
            outlineVariant = _TextAuxiliaryDark.copy(alpha = 0.12f),
            error = Color(0xFFFF8080),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = Color(0xFFEDF2F7),
            inverseOnSurface = Color(0xFF2D3748),
            inversePrimary = _Primary,
        )
    }

private fun brighten(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.65f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 1.4f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun LootArchiveTheme(
    themeMode: String = "system",
    primaryColor: Int = 0xFFFF7A3D.toInt(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
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
