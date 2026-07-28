package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.R

// ═══════════════════════════════════════════════════════════════
//  v5.0 字体系统 — Warm Glassmorphism
//  标题: Fredoka (圆润友好 — 已内嵌)
//  正文: Nunito (柔和清晰 — 已内嵌)
//  数字: Platform Monospace (JetBrains Mono 等效，等宽数字)
// ═══════════════════════════════════════════════════════════════

val FredokaFont = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

val NunitoFont = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

// v5.0: 等宽数字字体 (用于价格、统计数据)
val MonoFont = FontFamily.Monospace

val AppTypography = Typography(
    // ── v5.0 大数值展示 — Fredoka Bold ──
    displayLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 38.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp
    ),
    // ── 页面标题 — Fredoka SemiBold ──
    headlineLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 24.sp
    ),
    // ── 卡片标题 — Fredoka Medium ──
    titleLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Medium,
        fontSize = 17.sp, lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    // ── 正文 — Nunito ──
    bodyLarge = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    // ── 标签/按钮 — Nunito Medium ──
    labelLarge = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp
    )
)
