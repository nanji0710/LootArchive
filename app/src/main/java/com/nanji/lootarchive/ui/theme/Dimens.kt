package com.nanji.lootarchive.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  尺寸 token —— 圆角与间距的唯一来源
//
//  改前实测：RoundedCornerShape 出现 107 次、**18 种**不同半径
//  （14dp×25、20dp×17、12dp×14、10dp×10、6dp×7…… 还有 1/2/3/5/9/15dp 这种孤例）；
//  间距 dp 值 **34 种**，含 90/104/120/140/186/210/216/224dp 这类魔法值。
//  同一套界面里 14 和 16 和 18 混用，肉眼说不清差别，但改起来要逐个翻。
//
//  收敛原则：**只保留能一眼分辨的档位**。相差 1~2dp 的"微调"在这个尺度上
//  没有任何感官意义，合并掉反而让相邻组件的圆角真正对齐。
// ═══════════════════════════════════════════════════════════════

/**
 * 圆角刻度：18 种 → 6 种。
 *
 * 合并去向（右侧是被吸收的旧值）：
 *  - [xs] 4dp ← 1/2/3dp（发丝级：进度条、细分隔线）
 *  - [sm] 8dp ← 5/6dp（小色块、骨架条）
 *  - [md] 12dp ← 9/10/14dp（缩略图、输入框、色块图例）
 *  - [lg] 16dp ← 15/18dp（内层盒子、小卡）
 *  - [xl] 20dp ← 22dp（**标准卡片**，与 GlassTier.CARD 一致）
 *  - [xxl] 28dp ← 24dp（弹层、大面板）
 */
object AppRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

/** 间距刻度：34 种 → 8 种。 */
object AppSpacing {
    /** 2dp —— 只有"贴在一起但不要合并"的极小间隔用。 */
    val hairline = 2.dp
    val xs = 4.dp
    val sm = 6.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 20.dp
    val xxxl = 24.dp
    /** 32dp —— 分节之间的大留白。 */
    val huge = 32.dp

    // ── 页面骨架刻度 ──
    //
    // 这三个是"所有页面都该一致"的布局量。改前实测各页面自定：
    // 边缘 6/8/10/12/14/16/18/20/36/40dp 混用，卡片间距 6/8/10/12/14/16dp。

    /** 页面左右边缘。所有一级页面统一用它，卡片起止线才对得齐。 */
    val pageEdge = 16.dp

    /** 页面顶部/底部内边距。 */
    val pageVertical = 12.dp

    /**
     * 列表底部给悬浮底栏让出的高度。
     *
     * 底栏是浮在内容之上的（不占布局高度），所以滚动列表必须自己留出这段，
     * 否则最后一项永远被底栏压住。改前 HomeScreen 用 140dp、
     * StatisticsScreen 用 100dp、MainScreen 用 90dp —— 三个页面三种值，
     * 而底栏的实际几何是固定的，取一个就够。
     */
    val bottomBarClearance = 120.dp
}

/**
 * 形状 token。**同一个用途永远只有一个形状**，不要再在调用点现写
 * `RoundedCornerShape(某个数)`。
 *
 * 顶部是对象而不是函数：这些形状在整个 App 生命周期里不变，
 * 每次重组新建一个 RoundedCornerShape 是白白的分配（虽然便宜，但没必要）。
 */
object AppShape {
    /** 标准卡片。 */
    val card = RoundedCornerShape(AppRadius.xl)

    /** 内层容器 / 次级卡片（卡片里再套一层的那种）。 */
    val panel = RoundedCornerShape(AppRadius.lg)

    /** 缩略图、输入框、色块。 */
    val thumb = RoundedCornerShape(AppRadius.md)

    /** 小色块 / 图例点。 */
    val chip = RoundedCornerShape(AppRadius.sm)

    /** 发丝级：进度条、骨架条。 */
    val bar = RoundedCornerShape(AppRadius.xs)

    /** 全圆胶囊。按百分比取，任何高度都成立。 */
    val pill = RoundedCornerShape(percent = 50)

    /** 弹层 / 大面板 / 底部抽屉的顶部圆角。比卡片更圆，用于"浮在内容之上"的层。 */
    val sheet = RoundedCornerShape(AppRadius.xxl)
}
