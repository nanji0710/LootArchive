package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── 静态色值（Theme.kt 构建 colorScheme 使用） ───
val _Primary = Color(0xFFD4A574)
val _PrimaryDark = Color(0xFFE6B886)
val _OnPrimary = Color(0xFFFFFFFF)
val _BackgroundLight = Color(0xFFF7F3EA)
val _BackgroundDark = Color(0xFF1A1A1A)
val _SurfaceLight = Color(0xFFFFFFFF)
val _SurfaceDark = Color(0xFF303030)
val _GlassBgLight = Color(0xCCFFFFFF)
val _GlassBgDark = Color(0xD5222222)
val _GlassBorderLight = Color(0x40FFFFFF)
val _GlassBorderDark = Color(0x33000000)
val _TextPrimaryLight = Color(0xFF333333)
val _TextSecondaryLight = Color(0xFF666666)
val _TextAuxiliaryLight = Color(0xFF999999)
val _TextPrimaryDark = Color(0xFFF5F5F5)
val _TextSecondaryDark = Color(0xFFD9D9D9)
val _TextAuxiliaryDark = Color(0xFFAAAAAA)

// ─── @Composable 主题感知色（读取 LootArchiveTheme 提供的 LocalDarkTheme） ───
@Composable fun Primary() = MaterialTheme.colorScheme.primary // 动态跟随用户设置的主色
@Composable fun TextPrimary() = if (LocalDarkTheme.current) _TextPrimaryDark else _TextPrimaryLight
@Composable fun TextSecondary() = if (LocalDarkTheme.current) _TextSecondaryDark else _TextSecondaryLight
@Composable fun TextAuxiliary() = if (LocalDarkTheme.current) _TextAuxiliaryDark else _TextAuxiliaryLight
fun OnPrimary() = _OnPrimary

// ─── 功能色（不随主题变化） ───
val WarrantyActive = Color(0xFF4CAF50)
val WarrantyExpiring = Color(0xFFFF9800)
val WarrantyExpired = Color(0xFFF44336)

// ─── 图表色板 ───
val ChartColors = listOf(
    Color(0xFFD4A574), Color(0xFF4CAF50), Color(0xFF2196F3),
    Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0),
    Color(0xFF00BCD4), Color(0xFF795548)
)
