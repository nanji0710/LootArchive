package com.nanji.lootarchive.ui.liquidglass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nanji.lootarchive.ui.theme.LocalDarkTheme

internal data class LiquidDialogEntry(
    val key: Any,
    val onDismissRequest: State<() -> Unit>,
    val content: State<@Composable (Backdrop) -> Unit>
)

class LiquidDialogHostState internal constructor() {
    private var currentEntry by mutableStateOf<LiquidDialogEntry?>(null)
    internal fun show(entry: LiquidDialogEntry) { currentEntry = entry }
    internal fun dismiss(key: Any) { if (currentEntry?.key === key) currentEntry = null }
    internal val entry: LiquidDialogEntry? get() = currentEntry
}

private val LocalLiquidDialogHostState = staticCompositionLocalOf<LiquidDialogHostState?> { null }

@Composable
fun rememberLiquidDialogHostState(): LiquidDialogHostState = remember { LiquidDialogHostState() }

@Composable
fun ProvideLiquidDialogHost(state: LiquidDialogHostState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLiquidDialogHostState provides state, content = content)
}

@Composable
fun LiquidDialogHost(
    state: LiquidDialogHostState,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val currentEntry = state.entry
    var retainedEntry by remember { mutableStateOf<LiquidDialogEntry?>(null) }
    val visibilityState = remember { MutableTransitionState(false) }

    LaunchedEffect(currentEntry?.key) {
        val entry = currentEntry
        if (entry != null) {
            retainedEntry = entry
            visibilityState.targetState = false
            withFrameNanos { }
            visibilityState.targetState = true
        } else {
            visibilityState.targetState = false
        }
    }

    LaunchedEffect(currentEntry?.key, visibilityState.currentState, visibilityState.isIdle) {
        if (currentEntry == null && visibilityState.isIdle && !visibilityState.currentState) {
            retainedEntry = null
        }
    }

    BackHandler(enabled = retainedEntry != null) { currentEntry?.onDismissRequest?.value?.invoke() }

    val dark = LocalDarkTheme.current
    val dimColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.56f)
        else Color(0xFF29293A).copy(alpha = 0.23f)
    val visible = visibilityState.targetState

    AnimatedVisibility(
        visibleState = visibilityState,
        modifier = modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None
    ) {
        val transition = this.transition
        val scrimAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) tween(180) else tween(140)
            },
            label = "scrimAlpha"
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        val dialogAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) tween(160) else tween(120)
            },
            label = "dialogAlpha"
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        val dialogScale by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    spring(dampingRatio = 0.6f, stiffness = 250f)
                } else {
                    tween(140)
                }
            },
            label = "dialogScale"
        ) { state -> if (state == EnterExitState.Visible) 1f else 0.90f }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha }
                .background(dimColor)
                .clickable(
                    enabled = visible,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { retainedEntry?.onDismissRequest?.value?.invoke() }
        )

        val entry = retainedEntry
        if (entry != null) {
            key(entry.key) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = dialogAlpha
                            scaleX = dialogScale
                            scaleY = dialogScale
                        }
                ) {
                    entry.content.value.invoke(backdrop)
                }
            }
        }
    }
}

@Composable
fun LiquidDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    shape: Shape = ContinuousRoundedRectangle(48.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    content: @Composable ColumnScope.() -> Unit
) {
    val hostState = LocalLiquidDialogHostState.current
    if (hostState != null) {
        val key = remember { Any() }
        val currentOnDismissRequest = rememberUpdatedState(onDismissRequest)
        val currentContent = rememberUpdatedState<@Composable (Backdrop) -> Unit>(
            { backdrop ->
                LiquidDialogSurface(
                    backdrop = backdrop,
                    modifier = modifier,
                    alignment = alignment,
                    shape = shape,
                    contentPadding = contentPadding,
                    content = content
                )
            }
        )
        DisposableEffect(hostState, key) {
            hostState.show(
                LiquidDialogEntry(
                    key = key,
                    onDismissRequest = currentOnDismissRequest,
                    content = currentContent
                )
            )
            onDispose { hostState.dismiss(key) }
        }
    } else {
        Dialog(onDismissRequest = onDismissRequest, properties = properties) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    ),
                contentAlignment = Alignment.Center
            ) {
                SolidDialogSurface(
                    modifier = modifier,
                    alignment = alignment,
                    shape = shape,
                    contentPadding = contentPadding,
                    content = content
                )
            }
        }
    }
}

@Composable
fun SolidDialogSurface(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    shape: Shape = ContinuousRoundedRectangle(48.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val containerColor = if (dark) Color(0xFF202124) else Color(0xFFF8F9FA)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        contentAlignment = alignment
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(shape)
                .background(containerColor)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Final)
                        }
                    }
                }
                .then(modifier)
                .padding(contentPadding),
            verticalArrangement = Arrangement.Top,
            content = content
        )
    }
}

@Composable
fun LiquidDialogSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    shape: Shape = ContinuousRoundedRectangle(48.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.72f)
        else Color(0xFFFAFAFA).copy(alpha = 0.82f)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        contentAlignment = alignment
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(
                            brightness = if (dark) 0f else 0.2f,
                            saturation = 1.5f
                        )
                        blur(if (dark) 8.dp.toPx() else 16.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Final)
                        }
                    }
                }
                .then(modifier)
                .padding(contentPadding),
            verticalArrangement = Arrangement.Top,
            content = content
        )
    }
}

@Composable
fun LiquidAlertDialog(
    onDismissRequest: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit = {},
    text: @Composable () -> Unit = {},
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {}
) {
    LiquidDialog(onDismissRequest = onDismissRequest) {
        icon?.invoke()
        Spacer(Modifier.height(8.dp))
        title()
        Spacer(Modifier.height(8.dp))
        text()
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
            dismissButton()
            confirmButton()
        }
    }
}
