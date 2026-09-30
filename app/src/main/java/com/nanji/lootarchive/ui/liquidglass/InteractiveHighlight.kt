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

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Adapted from AndroidLiquidGlass 1.0.0 demo, commit
 * eada9a8600142c1bd6a5c5fc143d06f599cdfaef.
 */
internal class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    val intensity: Float = 1f,
    val radiusMultiplier: Float = 1.5f,
    /**
     * 系统「减弱动效」。这套高光有两条弹簧，同样不受 Compose 的动画时长缩放
     * 影响（弹簧是物理积分，没有时长），所以必须显式退化，否则开了减弱动效
     * 之后按压高光仍会跟着手指慢慢移。置 true 时改用 snap()。
     */
    val reduceMotion: Boolean = false,
) {

    private fun <T> spec(normal: AnimationSpec<T>): AnimationSpec<T> =
        if (reduceMotion) snap() else normal

    private val pressProgressAnimationSpec =
        spec(spring(0.5f, 300f, 0.001f))
    private val positionAnimationSpec =
        spec(spring(0.5f, 300f, Offset.VisibilityThreshold))

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
                    drawRect(
                        Color.White.copy(0.08f * progress * intensity),
                        blendMode = BlendMode.Plus
                    )
                    shader.apply {
                        val highlightPosition =
                            position(size, positionAnimation.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform(
                            "color",
                            Color.White.copy(0.15f * progress * intensity).toArgb()
                        )
                        setFloatUniform("radius", size.minDimension * radiusMultiplier)
                        setFloatUniform(
                            "position",
                            highlightPosition.x.fastCoerceIn(0f, size.width),
                            highlightPosition.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        ShaderBrush(shader),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.25f * progress * intensity),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch {
                            pressProgressAnimation.animateTo(
                                1f,
                                pressProgressAnimationSpec
                            )
                        }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch {
                            pressProgressAnimation.animateTo(
                                0f,
                                pressProgressAnimationSpec
                            )
                        }
                        launch {
                            positionAnimation.animateTo(
                                startPosition,
                                positionAnimationSpec
                            )
                        }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch {
                            pressProgressAnimation.animateTo(
                                0f,
                                pressProgressAnimationSpec
                            )
                        }
                        launch {
                            positionAnimation.animateTo(
                                startPosition,
                                positionAnimationSpec
                            )
                        }
                    }
                }
            ) { change, _ ->
                animationScope.launch {
                    positionAnimation.snapTo(change.position)
                }
            }
        }
}
