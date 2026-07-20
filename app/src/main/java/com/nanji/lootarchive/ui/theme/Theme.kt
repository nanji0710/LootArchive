package com.nanji.lootarchive.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = _Primary, onPrimary = _OnPrimary,
    secondary = _TextSecondaryLight, background = _BackgroundLight, surface = _SurfaceLight,
    surfaceVariant = _GlassBgLight,
    onBackground = _TextPrimaryLight, onSurface = _TextPrimaryLight, onSurfaceVariant = _TextSecondaryLight,
    outline = _GlassBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = _PrimaryDark, onPrimary = Color(0xFF1A1A1A),
    secondary = _TextSecondaryDark, background = _BackgroundDark, surface = _SurfaceDark,
    surfaceVariant = _GlassBgDark,
    onBackground = _TextPrimaryDark, onSurface = _TextPrimaryDark, onSurfaceVariant = _TextSecondaryDark,
    outline = _GlassBorderDark
)

@Composable
fun LootArchiveTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "dark" -> true; "light" -> false; else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController((view.context as Activity).window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
