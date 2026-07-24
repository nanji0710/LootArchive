package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun ColorWheel(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    size: Float = 280f
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.toPx() }
    val center = sizePx / 2f
    val radius = center - 12f

    // Convert current color to HSV to set initial picker position
    var selectedPos by remember { mutableStateOf(Offset.Zero) }

    // Calculate initial position from current color
    LaunchedEffect(currentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
        val angle = hsv[0] * PI.toFloat() / 180f
        val dist = hsv[1] * radius
        selectedPos = Offset(
            center + dist * cos(angle),
            center + dist * sin(angle)
        )
    }

    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(size.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val dx = pos.x - center
                            val dy = pos.y - center
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                            val angle = atan2(dy, dx)
                            selectedPos = Offset(
                                center + dist * cos(angle),
                                center + dist * sin(angle)
                            )
                            val saturation = dist / radius
                            val hue = ((angle * 180.0 / PI).toFloat() + 360f) % 360f
                            onColorChanged(Color.hsv(hue, saturation.coerceIn(0f, 1f), 1f))
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val pos = change.position
                            val dx = pos.x - center
                            val dy = pos.y - center
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                            val angle = atan2(dy, dx)
                            selectedPos = Offset(
                                center + dist * cos(angle),
                                center + dist * sin(angle)
                            )
                            val saturation = dist / radius
                            val hue = ((angle * 180.0 / PI).toFloat() + 360f) % 360f
                            onColorChanged(Color.hsv(hue, saturation.coerceIn(0f, 1f), 1f))
                        }
                    }
            ) {
                // Draw HSV color wheel
                val step = 2f
                for (y in 0..sizePx.toInt() step step.toInt()) {
                    for (x in 0..sizePx.toInt() step step.toInt()) {
                        val px = x.toFloat() - center
                        val py = y.toFloat() - center
                        val dist = sqrt(px * px + py * py)
                        if (dist <= radius) {
                            val angle = (atan2(py, px) * 180.0 / PI).toFloat()
                            val hue = (angle + 360f) % 360f
                            val sat = (dist / radius).coerceIn(0f, 1f)
                            drawRect(
                                color = Color.hsv(hue, sat, 1f),
                                topLeft = Offset(x.toFloat(), y.toFloat()),
                                size = Size(step, step)
                            )
                        }
                    }
                }

                // Draw selection indicator
                val indicatorPos = if (selectedPos == Offset.Zero) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
                    val angle = hsv[0] * PI.toFloat() / 180f
                    val dist = hsv[1] * radius
                    Offset(center + dist * cos(angle), center + dist * sin(angle))
                } else selectedPos

                // Selection ring
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = indicatorPos,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = 10f,
                    center = indicatorPos,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Color preview + hex
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(currentColor, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "#${Integer.toHexString(currentColor.toArgb()).substring(2).uppercase()}",
                fontSize = 14.sp,
                color = Color(currentColor.toArgb())
            )
        }
    }
}
