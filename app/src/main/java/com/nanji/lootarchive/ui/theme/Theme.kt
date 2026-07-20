package com.nanji.lootarchive.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = GlassBackgroundLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5C6B73)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryVariant,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = GlassBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF0F0F0),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF0F0F0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0B0B0)
)

@Composable
fun LootArchiveTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
