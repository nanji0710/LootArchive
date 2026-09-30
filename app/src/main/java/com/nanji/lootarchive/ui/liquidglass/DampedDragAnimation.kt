/*
 * Copyright (C) 2026 sky-shunfengjun
 *
 * SPDX-License-Identifier: GPL-3.0-only AND Apache-2.0
 *
 * This file is part of RiseDiary. RiseDiary's own code is licensed
 * under GPL-3.0-only (see LICENSE).
 *
 * Portions of this file are adapted and modified from AndroidLiquidGlass
 * by Kyant0 (https://github.com/Kyant0/AndroidLiquidGlass), which is
 * licensed under the Apache License, Version 2.0. Those portions remain
 * subject to Apache-2.0 (see LICENSES/Apache-2.0.txt).
 */
package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Adapted from AndroidLiquidGlass 1.0.0 demo, commit
 * eada9a8600142c1bd6a5c5fc143d06f599cdfaef.
 */
internal class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    private val consumeDragChanges: Boolean = false,
    private val consumeInitialDown: Boolean = true,
    /**
     * 系统是否开启了「减弱动效」。
     *
     * 这套物理**必须**显式处理它：Compose 的动画时长缩放（MotionDurationScale）
     * 只作用于带时长的 spec（tween 那一类），而弹簧是物理积分、没有时长可言，
     * 所以系统关掉动画之后这些弹簧照旧满帧弹 —— 底栏拖拽镜头、按压形变、
     * 图标缩放全都不会停。置 true 时改用 snap()，状态仍然是即时生效的，
     * 只是不再有过渡过程。
     */
    private val reduceMotion: Boolean = false,
) {

    // 减弱动效时一律瞬时到位。取值时再快进，避免把中间态画到屏幕上。
    private fun <T> spec(normal: AnimationSpec<T>): AnimationSpec<T> =
        if (reduceMotion) snap() else normal

    private val valueAnimationSpec =
        spec(spring(1f, 1000f, visibilityThreshold))
    private val velocityAnimationSpec =
        spec(spring(0.5f, 300f, visibilityThreshold * 10f))
    private val pressProgressAnimationSpec =
        spec(spring(1f, 1000f, 0.001f))
    private val scaleXAnimationSpec =
        spec(spring(0.6f, 250f, 0.001f))
    private val scaleYAnimationSpec =
        spec(spring(0.7f, 250f, 0.001f))

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float
        get() = (value - valueRange.start) /
            (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { change, dragAmount ->
            onDrag(size, dragAmount)
            if (
                shouldConsumeDragChange(
                    consumeDragChanges = consumeDragChanges,
                    consumeInitialDown = consumeInitialDown,
                    hasMovement = dragAmount != Offset.Zero
                )
            ) {
                change.consume()
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold =
                    (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch {
                valueAnimation.animateTo(targetValue, valueAnimationSpec) {
                    updateVelocity()
                }
            }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity =
            velocityTracker.calculateVelocity().x /
                (valueRange.endInclusive - valueRange.start)
        animationScope.launch {
            velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
        }
    }
}

internal fun shouldConsumeDragChange(
    consumeDragChanges: Boolean,
    consumeInitialDown: Boolean,
    hasMovement: Boolean
): Boolean =
    consumeDragChanges && (consumeInitialDown || hasMovement)
