package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.R

// ═══════════════════════════════════════════════════════════════
//  Playful Creative 字体系统
//  标题: Fredoka — 圆润友好年轻
//  正文: Nunito  — 柔和清晰可读
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

val AppTypography = Typography(
    // ── 大标题 — Fredoka Bold ──
    displayLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp
    ),
    // ── 页面标题 — Fredoka SemiBold ──
    headlineLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 32.sp
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
