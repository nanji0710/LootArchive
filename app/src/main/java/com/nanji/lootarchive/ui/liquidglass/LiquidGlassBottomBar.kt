/*
 * Adapted from AndroidLiquidGlass 1.0.0 demo by Kyant0
 * (https://github.com/Kyant0/AndroidLiquidGlass), Apache-2.0.
 */
package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.LocalReduceMotion
import com.nanji.lootarchive.ui.theme.MotionSpec
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

internal val LocalLiquidBottomTabSampling =
    staticCompositionLocalOf { false }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
internal fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 64.dp,
    contentPadding: Dp = 4.dp,
    showSelectionShadow: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.14f)
        else Color(0xFFFBF9F6).copy(alpha = 0.14f)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - (contentPadding * 2).toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    (contentPadding * 2).toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        var pendingCallback by remember { mutableStateOf<Int?>(null) }
        // 减弱动效时把弹簧换成瞬时（见 DampedDragAnimation.reduceMotion）。
        // 它要进 remember 的键：开了减弱动效之后必须重建这个物理对象，
        // 否则一直用着按旧值构造的 spec。
        val reduceMotion = LocalReduceMotion.current
        val dampedDragAnimation = remember(animationScope, tabsCount, reduceMotion) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                reduceMotion = reduceMotion,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    if (targetIndex != selectedTabIndex()) {
                        pendingCallback = targetIndex
                    }
                    animationScope.launch {
                        // 面板回弹归位：走 token 里的跟手弹簧
                        offsetAnimation.animateTo(
                            0f,
                            MotionSpec.draggable()
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    if (index != currentIndex) {
                        currentIndex = index
                        dampedDragAnimation.animateToValue(index.toFloat())
                    }
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                }
        }
        LaunchedEffect(pendingCallback) {
            val index = pendingCallback ?: return@LaunchedEffect
            withFrameNanos {}
            onTabSelected(index)
            if (pendingCallback == index) pendingCallback = null
        }

        val selectionHeightPx = with(LocalDensity.current) { (containerHeight - contentPadding * 2).toPx() }
        val interactiveHighlight = remember(animationScope, selectionHeightPx, reduceMotion) {
            InteractiveHighlight(
                animationScope = animationScope,
                reduceMotion = reduceMotion,
                position = { size, offset ->
                    val lensCenterX =
                        if (isLtr) {
                            (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        } else {
                            size.width -
                                (dampedDragAnimation.value + 0.5f) * tabWidth +
                                panelOffset
                        }
                    Offset(
                        lensCenterX + (offset.x - tabWidth / 2f),
                        size.height / 2f + (offset.y - selectionHeightPx / 2f)
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(containerHeight)
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            },
            LocalLiquidBottomTabSampling provides true
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(containerHeight - contentPadding * 2)
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = contentPadding)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = if (showSelectionShadow) {
                        {
                            val progress = dampedDragAnimation.pressProgress
                            Shadow(alpha = progress)
                        }
                    } else null,
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (dark) Color.White.copy(0.1f)
                            else Color.Black.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(containerHeight - contentPadding * 2)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

data class LiquidGlassTab(val label: String, val icon: ImageVector, val filledIcon: ImageVector)

@Composable
fun LiquidGlassBottomBar(
    tabs: List<LiquidGlassTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    var visualSelectedTabIndex by rememberSaveable { mutableIntStateOf(selectedTabIndex) }
    var pendingNavigationIndex by remember { mutableStateOf<Int?>(null) }
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    LaunchedEffect(selectedTabIndex) {
        visualSelectedTabIndex = selectedTabIndex
    }
    LaunchedEffect(pendingNavigationIndex) {
        val index = pendingNavigationIndex ?: return@LaunchedEffect
        withFrameNanos {}
        currentOnTabSelected(index)
        if (pendingNavigationIndex == index) pendingNavigationIndex = null
    }
    val selectTab: (Int) -> Unit = { index ->
        if (index != visualSelectedTabIndex) {
            visualSelectedTabIndex = index
            pendingNavigationIndex = index
        }
    }

    val contentColor = if (!LocalDarkTheme.current) Color.Black else Color.White
    val iconColorFilter = ColorFilter.tint(contentColor)

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { visualSelectedTabIndex },
            onTabSelected = selectTab,
            backdrop = backdrop,
            tabsCount = tabs.size,
            modifier = Modifier
                .weight(1f)
                .selectableGroup()
        ) {
            tabs.forEachIndexed { index, tab ->
                LiquidBottomTab(
                    { selectTab(index) },
                    Modifier.semantics { selected = index == visualSelectedTabIndex }
                ) {
                    val sampling = LocalLiquidBottomTabSampling.current
                    Icon(
                        rememberVectorPainter(if (sampling) tab.filledIcon else tab.icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(colorFilter = iconColorFilter)
                    )
                    BasicText(
                        tab.label,
                        style = TextStyle(contentColor, 12.sp)
                    )
                }
            }
        }
    }
}

data class LiquidSegmentOption(val label: String, val icon: ImageVector)

@Composable
fun LiquidSegmentedControl(
    options: List<LiquidSegmentOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 64.dp,
    contentPadding: Dp = 4.dp,
    showIcons: Boolean = true,
    labelFontSize: TextUnit = 13.sp,
    showSelectionShadow: Boolean = true,
) {
    if (options.isEmpty()) return

    var visualSelectedIndex by rememberSaveable(options.size) { mutableIntStateOf(selectedIndex) }
    var pendingIndex by remember { mutableStateOf<Int?>(null) }
    val currentOnSelected by rememberUpdatedState(onSelected)

    LaunchedEffect(selectedIndex) {
        visualSelectedIndex = selectedIndex
    }
    LaunchedEffect(pendingIndex) {
        val index = pendingIndex ?: return@LaunchedEffect
        withFrameNanos {}
        currentOnSelected(index)
        if (pendingIndex == index) pendingIndex = null
    }
    val select: (Int) -> Unit = { index ->
        if (index != visualSelectedIndex && index in options.indices) {
            visualSelectedIndex = index
            pendingIndex = index
        }
    }

    val contentColor = if (!LocalDarkTheme.current) Color.Black else Color.White
    val iconColorFilter = ColorFilter.tint(contentColor)

    LiquidBottomTabs(
        selectedTabIndex = { visualSelectedIndex },
        onTabSelected = select,
        backdrop = backdrop,
        tabsCount = options.size,
        modifier = modifier.selectableGroup(),
        containerHeight = containerHeight,
        contentPadding = contentPadding,
        showSelectionShadow = showSelectionShadow
    ) {
        options.forEachIndexed { index, option ->
            LiquidBottomTab(
                { select(index) },
                Modifier.semantics { selected = index == visualSelectedIndex }
            ) {
                if (showIcons) {
                    Icon(
                        rememberVectorPainter(option.icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(colorFilter = iconColorFilter)
                    )
                }
                BasicText(
                    option.label,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = labelFontSize,
                        fontWeight = if (index == visualSelectedIndex) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}
