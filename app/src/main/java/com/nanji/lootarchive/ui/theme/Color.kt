package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
//  v5.0 Warm Glassmorphism 色板
//  暖象牙底 + 毛玻璃卡片 + 琥珀点缀
//  设计来源: UI/UX Pro Max — Warm Glassmorphism + Bento Grid
// ═══════════════════════════════════════════════════════════════

// ── 主色 (暖琥珀系) ──
val _Primary = Color(0xFFE8782A)            // 暖琥珀 — 有温度的强调色
val _PrimaryDark = Color(0xFFF5995C)        // 深色模式提亮
val _OnPrimary = Color(0xFFFFFFFF)

val _Secondary = Color(0xFF7C3AED)          // 优雅紫 — 次强调
val _OnSecondary = Color(0xFFFFFFFF)

// ── Glass 底色体系 ──
val _BackgroundLight = Color(0xFFFBF9F6)    // 暖象牙白 — 有温度的画布
val _BackgroundDark = Color(0xFF0C0C10)      // 深暖黑

val _SurfaceLight = Color(0xFFFFFFFF)        // 纯白表面
val _SurfaceDark = Color(0xFF1C1C24)         // 深紫灰

val _CardLight = Color(0xFFFEFDFB)           // 卡片微暖白
val _CardDark = Color(0xFF242430)            // 卡片深灰

// ── Glass 专用色 (毛玻璃卡片/面板) ──
val _GlassLight = Color(0xAAFFFFFF)          // 浅色毛玻璃 (65% opacity)
val _GlassDark = Color(0xB21C1C24)           // 深色毛玻璃 (70% opacity)

val _GlassBorderLight = Color(0x80FFFFFF)     // 玻璃边框浅色
val _GlassBorderDark = Color(0x14FFFFFF)      // 玻璃边框深色

val _NavGlassLight = Color(0xBFFFFFFF)        // 导航毛玻璃 (75% opacity)
val _NavGlassDark = Color(0xCC14141C)         // 导航深色 (80% opacity)

// ── 文字色 (暖色调灰阶) ──
val _TextPrimaryLight = Color(0xFF1C1917)    // 暖黑
val _TextSecondaryLight = Color(0xFF78716C)  // 暖石灰
val _TextAuxiliaryLight = Color(0xFFA8A29E)  // 暖浅灰

val _TextPrimaryDark = Color(0xFFF0ECE6)     // 暖白
val _TextSecondaryDark = Color(0xFFA8A29E)   // 暖石灰
val _TextAuxiliaryDark = Color(0xFF78716C)   // 暖暗灰

// ── @Composable 主题感知色 ──
@Composable fun Primary() = MaterialTheme.colorScheme.primary
@Composable fun Secondary() = _Secondary
@Composable fun TextPrimary() = if (LocalDarkTheme.current) _TextPrimaryDark else _TextPrimaryLight
@Composable fun TextSecondary() = if (LocalDarkTheme.current) _TextSecondaryDark else _TextSecondaryLight
@Composable fun TextAuxiliary() = if (LocalDarkTheme.current) _TextAuxiliaryDark else _TextAuxiliaryLight
fun OnPrimary() = _OnPrimary

// ── Glass 背景色 ──
@Composable fun GlassBg() = if (LocalDarkTheme.current) _GlassDark else _GlassLight
@Composable fun GlassBorder() = if (LocalDarkTheme.current) _GlassBorderDark else _GlassBorderLight
@Composable fun NavGlassBg() = if (LocalDarkTheme.current) _NavGlassDark else _NavGlassLight
@Composable fun CardBg() = if (LocalDarkTheme.current) _CardDark else _CardLight

// ── 功能色 ──
val WarrantyActive = Color(0xFF10B981)
val WarrantyExpiring = Color(0xFFF59E0B)
val WarrantyExpired = Color(0xFFEF4444)
val SemanticInfo = Color(0xFF3B82F6)

// ── 图表色板 (12色，暖色调优先) ──
val ChartColors = listOf(
    Color(0xFFE8782A), Color(0xFF7C3AED), Color(0xFF10B981),
    Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF84CC16),
    Color(0xFFF97316), Color(0xFF6366F1), Color(0xFF14B8A6)
)

// ── 渐变色 (用于 Hero / 照片区渐变) ──
val GradientStartLight = Color(0xFFFFF8F0)
val GradientEndLight = Color(0xFFFFF0E0)
val GradientStartDark = Color(0xFF1A1410)
val GradientEndDark = Color(0xFF181008)
