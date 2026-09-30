package com.nanji.lootarchive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.R

// ═══════════════════════════════════════════════════════════════
//  v5.0 字体系统 — Warm Glassmorphism
//  标题: Fredoka (圆润友好 — 已内嵌)
//  正文: Nunito (柔和清晰 — 已内嵌)
//  数字: Platform Monospace (JetBrains Mono 等效，等宽数字)
//
//  ── v6.10 字号全面 token 化 ──
//
//  改前：这套 AppTypography 定义得挺完整，但**全项目 0 处引用**——
//  `MaterialTheme.typography` 一次都没出现过，所有文字都在调用点现写
//  `fontSize = N.sp`，实测 253 处、20 种字号（9~90sp），
//  外加 87 处零散的 `fontFamily = FredokaFont`。结果就是「字号」这件事
//  没有任何全局答案，改一次视觉要翻 20 个文件。
//
//  现在：调用点统一写 `style = MaterialTheme.typography.Xxx`，
//  这里就是唯一的字号来源。**新代码不要再写 fontSize。**
//
//  梯子经过压缩以覆盖真实用到的档位（本 App 最大 UI 文字 38sp，
//  没有正文超过 16sp 的场景），所以 display 一档比 Material 规范小。
//  取值是"能一眼分辨"的档，相邻档差 ≥1sp：
//
//    displayLarge 38 · displayMedium 30 · displaySmall 26
//    headlineLarge 24 · headlineMedium 22 · headlineSmall 18
//    titleLarge 20 · titleMedium 17 · titleSmall 15
//    bodyLarge 16 · bodyMedium 14 · bodySmall 13
//    labelLarge 15 · labelMedium 13 · labelSmall 12
//
//  旧字号 → 槽位 的对照（迁移时按此表批量替换）：
//    9/10/11/12sp → labelSmall   13sp → bodySmall     14sp → bodyMedium
//    15sp         → labelLarge   16sp → bodyLarge     17sp → titleMedium
//    18sp         → headlineSmall 20sp → titleLarge   22/23sp → headlineMedium
//    24sp         → headlineLarge 26sp → displaySmall 30sp → displayMedium
//    35/36/38sp   → displayLarge
//
//  注：调用点若同时写了 fontWeight / fontFamily，会**覆盖**槽位的对应属性
//  （Compose 的 Text 把这些参数当作 style 之上的 override），
//  所以已有的字重/字族声明在迁移后仍然生效，不会走样。
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

/**
 * 装饰性巨型字符（详情页无照片时那张"首字"水印）的字号。
 *
 * 90sp 是**版面元素**而不是排版层级 —— 它随容器缩放、只出现一次，
 * 没有第二个地方会用到。塞进 Typography 会污染那套梯子的语义，
 * 所以单独起个名字放在这里，至少让"90 这个数从哪来"有个出处。
 */
val DecorativeWatermarkSize: TextUnit = 90.sp

val AppTypography = Typography(
    // ── 大数值展示 — Fredoka Bold ──
    displayLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 38.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp
    ),
    // 26sp：原本 headlineLarge 的位置。本 App 没有比 38 更大的正文层级，
    // display 三档整体压小一档以覆盖真实的"大标题/大数字"用值。
    displaySmall = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 34.sp
    ),
    // ── 页面标题 — Fredoka SemiBold ──
    headlineLarge = TextStyle(
        fontFamily = FredokaFont, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp
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
        fontSize = 12.sp, lineHeight = 16.sp
    )
)
