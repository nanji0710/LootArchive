package com.nanji.lootarchive.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ═══════════════════════════════════════════════════════════════
//  外观 token —— 「视效模式」与「图标风格」的唯一来源
//
//  这两个开关都要求「一处切换、全 App 生效」，所以不放在任何一个
//  ViewModel 里，而是各占一个 CompositionLocal，由 MainActivity 从
//  DataStore 读出后注入（见 LootArchiveTheme 的 visualEffectMode /
//  iconStyle 参数）。任何界面读 LocalVisualEffect / LocalIconStyle 即可，
//  不需要再往下传参数。
// ═══════════════════════════════════════════════════════════════

/**
 * 视效模式：整机性能档位，而不是「关掉某个动画」的零散开关。
 *
 * 三档对应三种真实的资源取舍：
 *  - [IMMERSIVE] 液态玻璃全量模糊 + 背景光斑 + 共享元素飞行；
 *  - [BALANCED] 玻璃与转场保留，常驻环境动效（光斑漂移）关闭；
 *  - [LITE] 玻璃退化为实色面板、共享元素退化为整屏平移、光斑关闭。
 *
 * 消费点只有三个（都在 MainScreen / 主题层），页面不需要各自判断：
 *  1. [LootArchiveTheme] 按 [glassBlurEnabled] 选玻璃色板（实色 / 半透明）；
 *  2. `backgroundBrush()` 在 [LITE] 下返回纯色而不是渐变；
 *  3. MainScreen 在 [heroMotionEnabled] 为 false 时不再启用共享元素配对。
 */
enum class VisualEffectMode(
    val key: String,
    val label: String,
    val summary: String,
    val detail: String,
) {
    IMMERSIVE(
        key = "immersive",
        label = "沉浸",
        summary = "全量玻璃 + 背景光斑",
        detail = "液态玻璃模糊、背景光斑与共享元素飞行全部开启，观感最完整，代价是每一帧都要重新取样与模糊。",
    ),
    BALANCED(
        key = "balanced",
        label = "均衡",
        summary = "玻璃保留，光斑收敛",
        detail = "保留玻璃质感与页面转场，关闭背景光斑这类常驻环境动效。绝大多数设备的推荐档位。",
    ),
    LITE(
        key = "lite",
        label = "省电",
        summary = "实色面板，动效从简",
        detail = "玻璃改为实色、关闭模糊采样与共享元素飞行，改用整屏平移。低端机、长续航与发热敏感场景最稳。",
    );

    /** 玻璃件是否走模糊采样（LITE 下改为实色面板）。 */
    val glassBlurEnabled: Boolean get() = this != LITE

    /** 常驻环境动效（关于页光斑、背景漂移）是否运行。 */
    val ambientEnabled: Boolean get() = this == IMMERSIVE

    /** 共享元素飞行（卡片 ↔ 详情）是否启用；false 时退回整屏平移。 */
    val heroMotionEnabled: Boolean get() = this != LITE

    companion object {
        fun fromKey(key: String?): VisualEffectMode =
            entries.firstOrNull { it.key == key } ?: BALANCED
    }
}

/** 当前视效模式。默认 BALANCED —— 与设置页的默认值保持一致。 */
val LocalVisualEffect = staticCompositionLocalOf { VisualEffectMode.BALANCED }

/**
 * 图标风格。
 *
 * 三档都从 material-icons-extended 里已有的圆角/线框/填充三套图形里取，
 * 所以「切换风格」不需要新增任何资源；[DUOTONE] 是「线框叠一层淡色填充」，
 * 由 [AppIcon] 在绘制时合成，同样零资源成本。
 */
enum class IconStyle(
    val key: String,
    val label: String,
    val summary: String,
) {
    ROUNDED("rounded", "圆润", "填充圆角图标，亲和活泼"),
    OUTLINED("outlined", "线性", "线框图标，轻盈克制"),
    DUOTONE("duotone", "双色", "线框 + 淡色底衬，层次更丰富");

    companion object {
        fun fromKey(key: String?): IconStyle = entries.firstOrNull { it.key == key } ?: ROUNDED
    }
}

/** 当前图标风格。 */
val LocalIconStyle = staticCompositionLocalOf { IconStyle.ROUNDED }

/**
 * 同一枚图标的三种画法。
 *
 * [filled] 默认取 [rounded]：material-icons-extended 里大量图标只有
 * Round/Filled 之一有实体，缺省时复用圆角那枚，避免调用点出现空图形。
 */
data class AppIconVariants(
    val rounded: ImageVector,
    val outlined: ImageVector,
    val filled: ImageVector = rounded,
)

/**
 * 全 App 的图标索引。
 *
 * 新增界面请从这里取图标，而不是直接写 `Icons.Rounded.Xxx`：
 * 只有经过这层索引的图标才会跟随「图标风格」设置变化。
 */
object AppIcons {
    val home = AppIconVariants(Icons.Rounded.Home, Icons.Outlined.Home)
    val stats = AppIconVariants(Icons.Rounded.PieChart, Icons.Outlined.PieChart)
    val assistant = AppIconVariants(Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome)
    val person = AppIconVariants(Icons.Rounded.Person, Icons.Outlined.Person)
    val settings = AppIconVariants(Icons.Rounded.Settings, Icons.Outlined.Settings)
    val category = AppIconVariants(Icons.Rounded.Category, Icons.Outlined.Category)
    val backup = AppIconVariants(Icons.Rounded.Backup, Icons.Outlined.Backup)
    val trash = AppIconVariants(Icons.Rounded.Delete, Icons.Outlined.Delete)
    val info = AppIconVariants(Icons.Rounded.Info, Icons.Outlined.Info)
    val update = AppIconVariants(Icons.Rounded.SystemUpdateAlt, Icons.Outlined.SystemUpdateAlt)
    val palette = AppIconVariants(Icons.Rounded.Palette, Icons.Outlined.Palette)
    val visualEffect = AppIconVariants(Icons.Rounded.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion)
    val iconStyle = AppIconVariants(Icons.Rounded.Interests, Icons.Outlined.Interests)
    val notifications = AppIconVariants(Icons.Rounded.Notifications, Icons.Outlined.Notifications)
    val storage = AppIconVariants(Icons.Rounded.Storage, Icons.Outlined.Storage)
    val currency = AppIconVariants(Icons.Rounded.Payments, Icons.Outlined.Payments)
    val school = AppIconVariants(Icons.Rounded.School, Icons.Outlined.School)
    val search = AppIconVariants(Icons.Rounded.Search, Icons.Outlined.Search)
    val add = AppIconVariants(Icons.Rounded.Add, Icons.Outlined.Add)
    val chevronRight = AppIconVariants(Icons.Rounded.ChevronRight, Icons.Outlined.ChevronRight)
    // AutoMirrored 的箭头在 automirrored.rounded 包里，必须单独 import（见文件头）。
    val arrowBack = AppIconVariants(Icons.AutoMirrored.Rounded.ArrowBack, Icons.Rounded.ArrowBack)
    val send = AppIconVariants(Icons.Rounded.Send, Icons.Outlined.Send)
    val chat = AppIconVariants(Icons.Rounded.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline)
    val shield = AppIconVariants(Icons.Rounded.Shield, Icons.Outlined.Shield)
    val cloud = AppIconVariants(Icons.Rounded.CloudDownload, Icons.Outlined.CloudDownload)
    val refresh = AppIconVariants(Icons.Rounded.Refresh, Icons.Outlined.Refresh)
    val check = AppIconVariants(Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle)
    val tune = AppIconVariants(Icons.Rounded.Tune, Icons.Outlined.Tune)
    val lightMode = AppIconVariants(Icons.Rounded.LightMode, Icons.Outlined.LightMode)
    val darkMode = AppIconVariants(Icons.Rounded.DarkMode, Icons.Outlined.DarkMode)
    val history = AppIconVariants(Icons.Rounded.History, Icons.Outlined.History)
    val sparkle = AppIconVariants(Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome)
    val bolt = AppIconVariants(Icons.Rounded.Bolt, Icons.Outlined.Bolt)
    val memory = AppIconVariants(Icons.Rounded.Memory, Icons.Outlined.Memory)
    val book = AppIconVariants(Icons.Rounded.MenuBook, Icons.Outlined.MenuBook)
    val link = AppIconVariants(Icons.Rounded.Link, Icons.Outlined.Link)
    val key = AppIconVariants(Icons.Rounded.Key, Icons.Outlined.Key)
    val image = AppIconVariants(Icons.Rounded.Image, Icons.Outlined.Image)
    val close = AppIconVariants(Icons.Rounded.Close, Icons.Outlined.Close)
}

/**
 * 按当前（或指定）图标风格绘制一枚 [AppIconVariants]。
 *
 * - [IconStyle.ROUNDED] 用填充图形；[IconStyle.OUTLINED] 用线框；
 * - [IconStyle.DUOTONE] 在填充图形上先铺一层低透明度作为「底色块」，
 *   再叠线框作为主形，得到双色观感（不引入新资源）。
 *
 * 尺寸由 [modifier] 决定（调用点照旧写 `Modifier.size(20.dp)`）。
 */
@Composable
fun AppIcon(
    variants: AppIconVariants,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    style: IconStyle = LocalIconStyle.current,
) {
    when (style) {
        IconStyle.ROUNDED -> Icon(variants.rounded, contentDescription, modifier, tint = tint)
        IconStyle.OUTLINED -> Icon(variants.outlined, contentDescription, modifier, tint = tint)
        IconStyle.DUOTONE -> Box(modifier) {
            Icon(
                variants.filled,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                // 0.45 而不是 0.22：22dp 的图标在 0.22 下几乎看不出底色，
                // 用户会以为「双色」和「线性」没区别 —— 这是实测反馈。
                tint = tint.copy(alpha = 0.45f),
            )
            Icon(
                variants.outlined,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = tint,
            )
        }
    }
}

/** 按风格取「填充 / 线框」中的一枚，给不接受变体对的场景（如 Compose 图标参数）用。 */
fun AppIconVariants.vector(style: IconStyle): ImageVector = when (style) {
    IconStyle.ROUNDED -> rounded
    IconStyle.OUTLINED -> outlined
    IconStyle.DUOTONE -> outlined
}
