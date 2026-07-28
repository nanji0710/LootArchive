package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
//  现代拟物风 (Neumorphism Soft UI Evolution) 色板
//  冷灰底色 + 双层光影 + 橙色点缀
//  设计来源: UI/UX Pro Max — Neumorphism (Mobile) + Soft UI Evolution
// ═══════════════════════════════════════════════════════════════

// ── 主色 ──
val _Primary = Color(0xFFFF7A3D)            // 暖橙 — 拟物风格的点睛色
val _PrimaryDark = Color(0xFFFFB380)        // 深色模式提亮
val _OnPrimary = Color(0xFFFFFFFF)

val _Accent = Color(0xFF5B7FFF)             // 柔和蓝 — 次要强调
val _OnAccent = Color(0xFFFFFFFF)

// ── 拟物底色（统一的浅灰基调 — 光影在此之上产生深度） ──
val _BackgroundLight = Color(0xFFEEF0F4)    // 冷灰底 — 拟物画布
val _BackgroundDark = Color(0xFF1A1D23)     // 深灰蓝黑底

val _SurfaceLight = Color(0xFFEEF0F4)       // 与背景同色 — 影子区分层级
val _SurfaceDark = Color(0xFF242830)        // 比背景略亮

val _CardLight = Color(0xFFF8F9FB)          // 卡片明显亮于背景 — 清晰层次
val _CardDark = Color(0xFF2E323A)

// ── 导航栏（略不透以保持可用性） ──
val _NavGlassLight = Color(0xFFEEF0F4)
val _NavGlassDark = Color(0xFF242830)

// ── 文字色（干净冷灰，避免暖色偏色） ──
val _TextPrimaryLight = Color(0xFF2D3748)    // 深板岩
val _TextSecondaryLight = Color(0xFF5A6678)  // 中灰
val _TextAuxiliaryLight = Color(0xFF8B95A5)  // 浅灰

val _TextPrimaryDark = Color(0xFFEDF2F7)     // 亮白灰
val _TextSecondaryDark = Color(0xFFA0AEC0)   // 中亮灰
val _TextAuxiliaryDark = Color(0xFF6B7A8D)   // 中暗灰

// ── @Composable 主题感知色 ──
@Composable fun Primary() = MaterialTheme.colorScheme.primary
@Composable fun Accent() = _Accent
@Composable fun TextPrimary() = if (LocalDarkTheme.current) _TextPrimaryDark else _TextPrimaryLight
@Composable fun TextSecondary() = if (LocalDarkTheme.current) _TextSecondaryDark else _TextSecondaryLight
@Composable fun TextAuxiliary() = if (LocalDarkTheme.current) _TextAuxiliaryDark else _TextAuxiliaryLight
fun OnPrimary() = _OnPrimary

// ── 功能色 ──
val WarrantyActive = Color(0xFF38A169)
val WarrantyExpiring = Color(0xFFED8936)
val WarrantyExpired = Color(0xFFE53E3E)

// ── 图表色板（与橙蓝主色协调） ──
val ChartColors = listOf(
    Color(0xFFFF7A3D), Color(0xFF5B7FFF), Color(0xFF38A169),
    Color(0xFFED8936), Color(0xFFE53E3E), Color(0xFF805AD5),
    Color(0xFF319795), Color(0xFFD69E2E), Color(0xFF3182CE),
    Color(0xFFDD6B20), Color(0xFF9F7AEA), Color(0xFF2B6CB0)
)
