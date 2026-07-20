package com.nanji.lootarchive.ui.theme

import androidx.compose.ui.graphics.Color

// ─── 浅色模式 ───
val Primary = Color(0xFFD4A574)              // 主强调金色
val OnPrimary = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF7F3EA)       // 暖白背景
val SurfaceLight = Color(0xFFFFFFFF)
val GlassBgLight = Color(0xCCFFFFFF)          // 80% 白，玻璃卡片底色
val GlassBorderLight = Color(0x40FFFFFF)      // 25% 白，玻璃边框

val TextPrimary = Color(0xFF333333)           // 主标题
val TextSecondary = Color(0xFF666666)         // 次级正文
val TextAuxiliary = Color(0xFF999999)         // 辅助小字

// ─── 深色模式 ───
val PrimaryDark = Color(0xFFE6B886)           // 深色金色
val BackgroundDark = Color(0xFF1A1A1A)
val SurfaceDark = Color(0xFF303030)
val GlassBgDark = Color(0xCC303030)           // 80% 深灰玻璃
val GlassBorderDark = Color(0x33000000)       // 20% 黑边框

val TextPrimaryDark = Color(0xFFF2F2F2)
val TextSecondaryDark = Color(0xFFCCCCCC)
val TextAuxiliaryDark = Color(0xFF888888)

// ─── 功能色 ───
val WarrantyActive = Color(0xFF4CAF50)
val WarrantyExpiring = Color(0xFFFF9800)
val WarrantyExpired = Color(0xFFF44336)
val OverlayMask = Color(0x33000000)           // 20% 遮罩

// ─── 图表分类色板 ───
val ChartColors = listOf(
    Color(0xFFD4A574), Color(0xFF4CAF50), Color(0xFF2196F3),
    Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0),
    Color(0xFF00BCD4), Color(0xFF795548)
)
