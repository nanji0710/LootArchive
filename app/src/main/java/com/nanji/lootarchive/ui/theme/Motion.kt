package com.nanji.lootarchive.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

// ═══════════════════════════════════════════════════════════════
//  动效 token —— 时长 / 曲线 / 弹簧 的唯一来源
//
//  改前：14 处 tween(、9 处 spring(、8 处 Animatable( 的时长全部散写在各页面里，
//  同一个动作在不同页面用了不同值（转场 220、弹窗 180/160/140、数字 600），
//  没有任何"这个动效该多快"的统一答案，改一处手感要翻十几个文件。
//
//  风格取向：偏活泼。曲线允许过冲（overshoot）与预判（anticipation），
//  而不是清一色的 ease-in-out 阻尼收敛。曲线数值取自 jicun（即存）——
//  那边对每条曲线的"意图"有实测记录，见下。
//
//  ⚠ 时长单位统一用 Int（毫秒），因为 Compose 的 tween/Animatable 都吃毫秒。
// ═══════════════════════════════════════════════════════════════

/** 时长刻度。只有这 6 档，新代码从这里挑，不要再写裸数字。 */
object MotionDuration {
    /** 状态切换：Tab 指示器滑动、颜色/透明度过渡。 */
    const val Quick = 180

    /** 常规出入场：弹层淡入、列表项出现、数值滚动。 */
    const val Medium = 260

    /** 页面级转场：二级页推进/退出。 */
    const val Page = 320

    /** 收起。比展开稍快更利落，但同样留回弹。 */
    const val Collapse = 420

    /** 展开。比收起慢：一次滑出一整块内容，太快像被弹开。 */
    const val Expand = 460

    /** 错峰入场的总时间线（每项在此基础上按索引偏移）。 */
    const val Stagger = 560

    /** 错峰步长：总时间线的百分比，0.22 ≈ 123ms。 */
    const val StaggerStep = 0.22f

    /**
     * 数字滚动（首页 Hero 金额/件数那种"数着涨上去"）。
     * 比交互动效慢一档是刻意的：它是**被观看**的，不是被操作的，
     * 260ms 看不清中间过程，600ms 才能读出"在涨"。
     */
    const val Count = 600

    /** 环境动效（关于页光斑、空状态浮动）：不是交互，时长自成一套。 */
    object Ambient {
        const val OrbSlow = 13000
        const val OrbMedium = 9000
        const val OrbFast = 7000
        const val Float = 2000
    }
}

/**
 * 曲线。**过冲与预判是刻意的**，不是配错了。
 *
 * jicun 的两条曲线带着明确的设计记录，直接沿用：
 *  - 展开的回弹要留在**末尾**：前段匀速铺开，最后冲到目标上面一点再落回。
 *    `easeOutBack` 那种"前段猛冲、末尾慢慢蹭"看着就是"一下就完了"。
 *  - 收起的回弹只能做成"先往回涨一点再缩"：高度没法缩得比标题行还短，
 *    所以让曲线前三分之一先下探（箱子先涨约 9%）再收到 0。
 */
object MotionCurve {
    /** 标准减速。默认无过冲，用于不做花活的位移/淡入。 */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** 出入场：快出慢收。 */
    val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** 对称缓入缓出。用于环境类动画。 */
    val EaseInOut: Easing = FastOutSlowInEasing

    /**
     * 展开：**末尾过冲**（y3 = 1.25）。
     * x 值仍是 0.35/0.45 落在 [0,1] 内 —— Compose 的 CubicBezierEasing 只约束 x，
     * y 允许超界，过冲才画得出来。
     */
    val Expand: Easing = CubicBezierEasing(0.35f, 0.30f, 0.45f, 1.25f)

    /**
     * 收起：**起始预判**（y2 = -0.50，曲线先往回走再收）。
     * 注意 Compose 侧用的时候要保证调用方不会因负值产生非法尺寸
     * （例如 heightFactor 必须 clamp 到 0 以上）。
     */
    val Collapse: Easing = CubicBezierEasing(0.70f, -0.50f, 0.40f, 1.0f)

    /** 强调：明显过冲，用于"弹一下"的入场（徽章、选中指示器）。 */
    val Emphasized: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** 线性。只给环境动画（背景光斑那种匀速漂移）用。 */
    val Linear: Easing = LinearEasing
}

/**
 * 弹簧规格。物理型动效（可拖拽、跟手）用这些，而不是 tween。
 *
 * **注意**：Compose 的弹簧**不响应**系统的"动画时长缩放"（`MotionDurationScale`
 * 只作用在带显式时长的 spec 上）。所以凡是用弹簧的地方，
 * 需要配合 [LocalReduceMotion] 自行降级，见 [rememberMotionSpec]。
 */
object MotionSpec {
    /** 活泼：明显过冲。选中指示器、图标弹跳。 */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.5f, stiffness = 400f)

    /** 标准：轻微过冲，跟手又不浮夸。 */
    fun <T> lively(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 400f)

    /** 沉稳：无过冲。大面积位移、卡片展开。 */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)

    /** 可拖拽镜头的跟手弹簧（底栏、开关）。 */
    fun <T> draggable(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 700f)
}

// ────────────────────────── 减弱动效 ──────────────────────────

/**
 * 系统是否开启了"减弱动效"（开发者选项里的动画缩放设为 0，或无障碍设置）。
 *
 * 为什么需要它：Compose 的 **tween 类**动画会自动跟随系统的动画缩放
 * （`MotionDurationScale` 为 0 时直接跳到终态），但 **spring 类**不会 ——
 * 弹簧是物理积分，没有"时长"这个概念可缩放。液态玻璃那套（底栏拖拽镜头、
 * 按压形变、高光）全是弹簧，所以系统开了减弱动效它们照旧满帧播放。
 *
 * 判断依据取 `ANIMATOR_DURATION_SCALE == 0`：这是 Android 官方"关闭动画"
 * 的表示方式，也是 Compose 自己读的那个值，两边口径一致。
 */
val LocalReduceMotion = compositionLocalOf { false }

private fun readReduceMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f
}.getOrDefault(false)

/**
 * 读取并**监听**系统的减弱动效开关。
 *
 * 监听是必要的：用户在系统设置里改了之后回到 App，进程还活着，
 * 只在启动时读一次会一直用旧值。
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    var reduce by remember(context) { mutableStateOf(readReduceMotion(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduce = readReduceMotion(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduce
}

/** 在子树里提供减弱动效状态。挂在 App 根部一次即可。 */
@Composable
fun ProvideReduceMotion(content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    CompositionLocalProvider(LocalReduceMotion provides reduce, content = content)
}

/**
 * 按当前"减弱动效"状态取规格：开启时退化成瞬时（[snap]），否则用传入的规格。
 *
 * 用法：
 * ```
 * val spec = rememberMotionSpec(MotionSpec.bouncy<Float>())
 * val scale by animateFloatAsState(target, animationSpec = spec)
 * ```
 */
@Composable
fun <T> rememberMotionSpec(spec: AnimationSpec<T>): AnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else spec

/**
 * [rememberMotionSpec] 的时长版：tween 按 [MotionDuration] 的档位取。
 *
 * 注意 tween 本身已经跟随系统动画缩放，这里再包一层是为了让**调用点**读起来
 * 统一（都用 rememberMotionSpecXxx 取规格），而不是因为它额外做了什么事。
 */
@Composable
fun <T> rememberTweenSpec(
    durationMillis: Int,
    easing: Easing = MotionCurve.Standard,
): AnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMillis = durationMillis, easing = easing)
