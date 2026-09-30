package com.nanji.lootarchive.ui.theme

// ═══════════════════════════════════════════════════════════════
//  这个文件原本装着 v5.0 的「GlassTier 五档规格」和 glassEffect Modifier。
//
//  迁移到 Kyant Backdrop（v6.8）之后两者都成了死代码，实测为 0 引用：
//    * GlassTier(NAV/CARD/DIALOG/FAB/SHEET) 唯一的"消费点"是别名 GlassCard，
//      而后者收下 tier 参数后**直接忽略**、转手调用 NeoCard
//      —— 所以那五档规格是文档而不是代码；
//    * Modifier.glassEffect() 全项目只有它自己的声明，没有任何调用点。
//
//  圆角现在有唯一来源：ui/theme/Dimens.kt 的 AppRadius / AppShape。
//  玻璃规格则真实存在于 ui/liquidglass/ 那 10 个组件的 drawBackdrop 参数里。
// ═══════════════════════════════════════════════════════════════
