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
val LocalPrimaryColor = staticCompositionLocalOf { Color(0xFFE8782A) }

// ══════════════════════════════════════════════════════════════
//  v5.0 Light ColorScheme — Warm Glassmorphism
//  暖象牙白底 + 毛玻璃卡片 + 琥珀点缀
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
            secondary = _Secondary,
            onSecondary = Color.White,
            secondaryContainer = _Secondary.copy(alpha = 0.12f),
            onSecondaryContainer = Color(0xFF1A0040),
            tertiary = _Secondary,
            onTertiary = Color.White,
            tertiaryContainer = _Secondary.copy(alpha = 0.10f),
            onTertiaryContainer = Color(0xFF1A0040),
            background = _BackgroundLight,
            onBackground = _TextPrimaryLight,
            surface = _SurfaceLight,
            onSurface = _TextPrimaryLight,
            surfaceVariant = _CardLight,
            onSurfaceVariant = _TextSecondaryLight,
            outline = _TextAuxiliaryLight.copy(alpha = 0.20f),
            outlineVariant = _TextAuxiliaryLight.copy(alpha = 0.10f),
            error = WarrantyExpired,
            onError = Color.White,
            errorContainer = WarrantyExpired.copy(alpha = 0.12f),
            onErrorContainer = Color(0xFF410002),
            inverseSurface = Color(0xFF1C1917),
            inverseOnSurface = Color(0xFFF0ECE6),
            inversePrimary = _PrimaryDark,
            scrim = Color.Black.copy(alpha = 0.45f),
        )
    }

// ══════════════════════════════════════════════════════════════
//  v5.0 Dark ColorScheme — Warm Glassmorphism Dark
//  深暖黑底 + 毛玻璃面板 + 提亮主色
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
            secondary = _Secondary,
            onSecondary = Color(0xFF1A0040),
            secondaryContainer = _Secondary.copy(alpha = 0.18f),
            onSecondaryContainer = Color(0xFFD0C0FF),
            tertiary = _Secondary,
            onTertiary = Color(0xFF1A0040),
            tertiaryContainer = _Secondary.copy(alpha = 0.15f),
            onTertiaryContainer = Color(0xFFD0C0FF),
            background = _BackgroundDark,
            onBackground = _TextPrimaryDark,
            surface = _SurfaceDark,
            onSurface = _TextPrimaryDark,
            surfaceVariant = _CardDark,
            onSurfaceVariant = _TextSecondaryDark,
            outline = _TextAuxiliaryDark.copy(alpha = 0.20f),
            outlineVariant = _TextAuxiliaryDark.copy(alpha = 0.10f),
            error = Color(0xFFFF8080),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = Color(0xFFF0ECE6),
            inverseOnSurface = Color(0xFF1C1917),
            inversePrimary = _Primary,
            scrim = Color.Black.copy(alpha = 0.55f),
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
    primaryColor: Int = 0xFFE8782A.toInt(),
    visualEffectMode: String = "balanced",
    iconStyle: String = "rounded",
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
    val effect = VisualEffectMode.fromKey(visualEffectMode)
    val icons = IconStyle.fromKey(iconStyle)
    // 视效模式只有一处出口：玻璃色板。LITE 下把半透明玻璃换成实色面板，
    // 所有玻璃件（底栏/按钮/开关/弹窗）自动跟着变实，页面无需各自判断。
    val glassColors = when {
        darkTheme && effect.glassBlurEnabled -> DarkGlassColors
        darkTheme -> DarkGlassColors.toOpaque()
        effect.glassBlurEnabled -> LightGlassColors
        else -> LightGlassColors.toOpaque()
    }
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalPrimaryColor provides Color(primaryColor),
        LocalGlassColors provides glassColors,
        LocalVisualEffect provides effect,
        LocalIconStyle provides icons
    ) {
        val cs = if (darkTheme) DarkColorScheme else LightColorScheme
        MaterialTheme(colorScheme = cs, typography = AppTypography, content = content)
    }
}

/**
 * 半透明玻璃色板 → 实色面板色板（「省电」视效模式用）。
 *
 * 只改「底」不改「字」：卡片/导航底换成与页面背景同族的实色，
 * 高光与描边归零，这样玻璃件不再需要背景采样也能读作一块完整面板。
 */
private fun GlassColorScheme.toOpaque(): GlassColorScheme = copy(
    glassBg = cardBg,
    navGlassBg = cardBg,
    glassBorder = textAuxiliary.copy(alpha = 0.10f),
    highlight = Color.Transparent,
    shadow = Color.Black.copy(alpha = if (textPrimary.luminanceSafe() < 0.3f) 0.25f else 0.06f),
)

/** 极简亮度判断：luminance 公式的近似，只用于选阴影透明度。 */
private fun Color.luminanceSafe(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
