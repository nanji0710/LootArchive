@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.nanji.lootarchive.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import com.nanji.lootarchive.ui.theme.AppShape
import com.nanji.lootarchive.ui.theme.CardBg
import com.nanji.lootarchive.ui.theme.MotionCurve
import com.nanji.lootarchive.ui.theme.MotionDuration
import kotlin.math.roundToInt

/**
 * 共享元素（「从哪来到哪去」）的身份字符串。
 *
 * 源端（首页卡片、我的页菜单行）与目标端（详情页首图、二级页底板）用同一个 key 配对，
 * 由 SharedTransitionLayout 负责两端测量与位移/尺寸插值；key 不匹配的那一端完全不加
 * modifier，等于没接入这套动画，零副作用。
 */
object NavKeys {
    // 物品卡片 ↔ 详情页：同一件物品的四个可对应元素各挂一个 key。
    // 分开挂键是关键——只挂一个键时整个卡片会被当成一张图片缩放，
    // 名称/金额/标签既不会各自飞到详情页的对应位置，也会被一起拉花。
    fun itemPhoto(id: Long) = "item:$id:photo"
    fun itemTitle(id: Long) = "item:$id:title"
    fun itemPrice(id: Long) = "item:$id:price"
    fun itemTag(id: Long) = "item:$id:tag"
    const val SETTINGS = "row:settings"
    const val CATEGORY = "row:category"
    const val BACKUP = "row:backup"
    const val RECYCLEBIN = "row:recyclebin"
    const val ABOUT = "row:about"
}

/** 本次导航的共享元素上下文：谁在飞、往哪个方向飞、用哪个 scope 飞。 */
class NavSharedScope(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
    val keys: Set<String>,
    /** true = 进入（展开），false = 返回（收起）；决定缓动曲线的方向。 */
    val forward: Boolean,
)

/**
 * 被点击卡片标题的字号（首页点击时写入）。
 *
 * 详情页标题用它作为字号插值的起点：`lerp(navSourceTitleSize, 详情页字号, 飞行进度)`，
 * 于是标题在飞行途中是**逐帧连续变大**的，而不是先瞬间跳到目标字号再开始位移。
 */
var navSourceTitleSize by mutableStateOf(15.sp)

val LocalNavShared = staticCompositionLocalOf<NavSharedScope?> { null }

@Composable
fun ProvideNavShared(
    shared: SharedTransitionScope,
    visibility: AnimatedVisibilityScope,
    keys: Set<String>,
    forward: Boolean,
    progress: () -> Float,
    content: @Composable () -> Unit,
) {
    val value = if (keys.isEmpty()) null else remember(shared, visibility, keys, forward) {
        NavSharedScope(shared, visibility, keys, forward)
    }
    CompositionLocalProvider(
        LocalNavShared provides value,
        LocalNavProgress provides progress,
        content = content,
    )
}

/**
 * 转场进度：0 = 还停在源页面那一端，1 = 已经落在目标页面那一端。
 *
 * 与 [NavSharedScope.flightProgress] 的区别：那个读的是 AnimatedVisibility 的
 * EnterExitState，页面一开始退场就归零（所以返回方向没法逐帧插值）；这个由 MainScreen
 * 的一条 tween 驱动，进入和返回都会平滑走完全程，两个方向都能逐帧插值字号。
 */
val LocalNavProgress = staticCompositionLocalOf<() -> Float> { { 0f } }

/** 当前转场进度（不在共享元素转场中时，由 MainScreen 给出「已落位」的稳态值）。 */
@Composable
fun navProgress(): Float = LocalNavProgress.current()

/**
 * 共享元素的飞行进度：0 = 还停在出发端（源形态），1 = 已经落位（目标形态）。
 *
 * 时长与曲线跟 sharedBounds 的 boundsTransform 完全一致（Hero + 方向化缓动），所以这个
 * 进度和元素的实际位移是同步的。
 */
@Composable
fun NavSharedScope.flightProgress(): Float {
    val progress by visibility.transition.animateFloat(
        transitionSpec = {
            tween(
                MotionDuration.Hero,
                easing = if (forward) MotionCurve.Standard else MotionCurve.Reverse,
            )
        },
        label = "navFlightProgress",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    return progress
}

/** 取当前导航的飞行进度；不在共享元素导航里时返回 1（= 目标形态，等于什么都没发生）。 */
@Composable
fun navFlightProgress(): Float = LocalNavShared.current?.flightProgress() ?: 1f

/**
 * 两端唯一助手：key 匹配才接入共享元素管线。
 *
 * 位移与尺寸插值交给框架（[SharedTransitionScope.sharedBounds]），所以这里只管
 * 「时长曲线、缩放方式、飞行期间的裁剪形状」三件事。
 *
 * ⚠️ 红线：飞行容器里只许放纯视觉（图片 / 纯色底 / 文字图标）。凡是采样页面级
 * backdrop 的液态玻璃件都必须留在容器外 —— 7.2.1 的启动闪退就是玻璃图层与
 * 渲染线程变换链互相引用递归（RenderThread SIGSEGV），别再踩。
 */
/**
 * 文字 / 胶囊类共享元素走 ScaleToBounds(Fit)：整块内容当成一个整体做**等比缩放**，
 * 所以飞行途中的**等效字号是从源端连续插值到目标端的** —— 这正是「文字从哪里来到哪里去」
 * 的关键（首页标题 14sp → 详情页大标题 34sp 是平滑放大，不是中途跳一下字号）。
 *
 * 线上一度改成 RemeasureToBounds（内容重新排版、字号恒定为代码里那份）：好处是落位瞬间
 * 与目标页那份文字完全重合，代价是**飞行第一帧就用目标端字号重新排版** —— 看起来就是
 * 「标题先瞬间变大/变小一次，然后才开始移动」，也就是用户报的闪烁。
 *
 * 之所以现在敢用 ScaleToBounds，是因为两端文字都接了 [navFlightProgress] 做字形交叉淡入：
 * 首页的截断形态（“多特蓝精灵 5…”）与详情页的完整形态互相淡入淡出，落位前看不到两层字。
 * 需要「一块像素整体缩放、允许裁切」的照片走 [navSharedPhoto]（Crop）。
 */
@Composable
fun Modifier.navSharedElement(
    key: String,
    shape: Shape = RectangleShape,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.Fit),
): Modifier {
    val nav = LocalNavShared.current ?: return this
    if (key !in nav.keys) return this
    val state = nav.shared.rememberSharedContentState(key = key)
    return with(nav.shared) {
        this@navSharedElement.sharedBounds(
            sharedContentState = state,
            animatedVisibilityScope = nav.visibility,
            boundsTransform = { _, _ ->
                // 返回用 Standard 的时间镜像：进入是「先快后缓」，返回必须也是「先缓后快」，
                // 否则两段运动的正反节奏对不上（用户实测原话：打开和返回曲线完全不一样）。
                tween(
                    MotionDuration.Hero,
                    easing = if (nav.forward) MotionCurve.Standard else MotionCurve.Reverse,
                )
            },
            resizeMode = resizeMode,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

/**
 * 照片/图片类共享元素：内容按 Crop 缩放去填飞行容器。
 *
 * 文字不能用这个——照片是「一块像素」，缩放它等于放大照片（自然）；文字若也按 Crop 缩放，
 * 宽高比一变就会被裁掉字头字尾。文字走默认的 ScaleToBounds(Fit)：等比放大、只留白不裁切。
 */
@Composable
fun Modifier.navSharedPhoto(key: String, shape: Shape = RectangleShape): Modifier =
    navSharedElement(key, shape, SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.Crop))

/**
 * 固定 UI 的「离场 / 回场」位移：按统一的转场进度把自身沿 Y 轴推出屏幕 / 拉回原位。
 *
 * 用**布局位移**而不是 graphicsLayer 是刻意的：这里挂的是采样页面 backdrop 的液态玻璃件，
 * 额外套一层图形图层有触发 7.2.1 那类「图层互相引用 → 渲染线程递归」的风险；布局位移不产生
 * 图层，等价于 Flutter 的 Positioned / Transform.translate，做到「真的移出屏幕」而非淡出。
 *
 * @param progress 0 = 原位（首页常态），1 = 完全移出屏幕
 * @param down true = 向下移出（底部导航 / 悬浮按钮），false = 向上移出（顶部搜索栏）
 * @param extra 在自身高度之外多走一点，保证彻底离开屏幕（不停留在边缘）
 */
fun Modifier.slideAway(progress: () -> Float, down: Boolean, extra: Dp = 0.dp): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val distance = placeable.height + extra.roundToPx()
        layout(placeable.width, placeable.height) {
            val p = progress().coerceIn(0f, 1f)
            placeable.place(0, ((if (down) distance else -distance) * p).roundToInt())
        }
    }

/**
 * 二级页统一底板：整页作为共享元素的「目标端」，从被点的那一行原地放大到铺满屏幕，
 * 返回时原路收回到那一行。
 *
 * 底板只在飞行期间可见（alpha 由本页的转场进度驱动，落位即淡出），所以页面静止时的
 * 外观与改动前完全一致。
 */
@Composable
fun NavSharedSurface(key: String, content: @Composable () -> Unit) {
    val nav = LocalNavShared.current
    val active = nav != null && key in nav.keys
    // 整页作为共享体时必须 RemeasureToBounds：页面在飞行中重新排版，而不是被当成一张图放大。
    Box(
        Modifier.fillMaxSize().navSharedElement(
            key, shape = AppShape.card,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    ) {
        if (active && nav != null) {
            // Visible = 本页已落位；PreEnter / PostExit = 正在飞 → 底板显形
            val plateAlpha by nav.visibility.transition.animateFloat { state ->
                if (state == EnterExitState.Visible) 0f else 1f
            }
            Box(Modifier.fillMaxSize().background(CardBg().copy(alpha = plateAlpha)))
        }
        content()
    }
}

/**
 * 首页「解体式退场 / 重新形成」的进度源：0 = 常态（原位、原始大小、不透明），1 = 已解体。
 *
 * 用**函数**而不是 Float 往外传：状态读取发生在绘制阶段（[dissolveOut] 的 graphicsLayer
 * 里），动画每帧只重绘，不触发首页网格重组。
 */
val LocalHomeDissolve = staticCompositionLocalOf<() -> Float> { { 0f } }

@Composable
fun ProvideHomeDissolve(progress: () -> Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHomeDissolve provides progress, content = content)
}

/**
 * 轻量解体：轻微缩小（1 → 0.9）+ 轻微上移 + 渐隐，只用 alpha / scale / translate 三件事。
 *
 * 不做模糊、不用 RenderEffect、不用粒子：这些都是额外离屏图层或 GPU 特效，代价高、
 * 收益低；上移 + 缩小 + 淡出已经足够产生「化开」的观感，且视觉焦点仍在飞行的那张卡片上。
 *
 * 与卡片 Hero 共用同一根时间轴（由调用方传入同一个进度），返回时反向播一遍就是「重新形成」。
 *
 * ⚠️ 红线：只套在页面内容上。采样页面级 backdrop 的液态玻璃件如果被套进会互相引用的
 * 图层里，会重演 7.2.1 的渲染线程递归闪退（见文件头说明）。
 */
fun Modifier.dissolveOut(
    progress: () -> Float,
    scaleTo: Float = 0.9f,
    lift: Dp = 10.dp,
): Modifier = this.graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    alpha = 1f - p
    val s = 1f + (scaleTo - 1f) * p
    scaleX = s
    scaleY = s
    translationY = -lift.toPx() * p
}

/**
 * 正在飞的物品卡片 id（本次导航是「卡片 ↔ 详情页」时非空）。
 *
 * 被点的那张卡片要留给共享元素自己飞，所以它不参与解体；用它把这张卡片排除掉。
 */
@Composable
fun navFlyingItemId(): Long? {
    val keys = LocalNavShared.current?.keys ?: return null
    return keys.firstOrNull { it.startsWith("item:") }
        ?.removePrefix("item:")
        ?.substringBefore(':')
        ?.toLongOrNull()
}
