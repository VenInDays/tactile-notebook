package com.tactile.notebook.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tactile.notebook.ui.theme.*
import kotlin.math.*

/**
 * "The Kinetic Dial" — A large rotary disk toolbar.
 * Only 25% of the disk is visible at the bottom-right corner.
 * Rotation gesture cycles through tools: Pen, Camera, Voice, Settings.
 */
enum class DialTool(val label: String, val symbol: String) {
    PEN("Pen", "\u270E"),
    CAMERA("Camera", "\u25C9"),
    VOICE("Voice", "\u25CB"),
    SETTINGS("Settings", "\u2699")
}

@Composable
fun KineticDial(
    modifier: Modifier = Modifier,
    onToolSelected: (DialTool) -> Unit = {},
    currentTool: DialTool = DialTool.PEN
) {
    val dialDiameter = 500.dp
    val dialRadius = dialDiameter / 2

    var targetRotation by remember(currentTool) {
        mutableFloatStateOf(currentTool.ordinal * 90f)
    }
    val animatedRotation = remember { Animatable(targetRotation) }

    var accumulatedAngle by remember { mutableFloatStateOf(0f) }
    var dragStartAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(targetRotation) {
        animatedRotation.animateTo(
            targetValue = targetRotation,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width.toFloat(), size.height.toFloat())
                        dragStartAngle = atan2(
                            offset.y - center.y,
                            offset.x - center.x
                        ).toDegrees()
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width.toFloat(), size.height.toFloat())
                        val currentAngle = atan2(
                            change.position.y - center.y,
                            change.position.x - center.x
                        ).toDegrees()
                        val delta = currentAngle - dragStartAngle
                        accumulatedAngle += delta
                        dragStartAngle = currentAngle

                        var snappedIndex = (accumulatedAngle / 90f).roundToInt()
                        snappedIndex = snappedIndex.coerceIn(0, DialTool.entries.size - 1)
                        val tool = DialTool.entries[snappedIndex]
                        if (tool != currentTool) {
                            onToolSelected(tool)
                        }
                    },
                    onDragEnd = {
                        var snappedIndex = (accumulatedAngle / 90f).roundToInt()
                        snappedIndex = snappedIndex.coerceIn(0, DialTool.entries.size - 1)
                        val tool = DialTool.entries[snappedIndex]
                        onToolSelected(tool)
                        targetRotation = snappedIndex * 90f
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width.toFloat(), size.height.toFloat())
                    val angle = atan2(
                        offset.y - center.y,
                        offset.x - center.x
                    ).toDegrees()

                    val tool = when {
                        angle in -45f..45f -> DialTool.CAMERA
                        angle in 45f..135f -> DialTool.PEN
                        angle in -135f..-45f -> DialTool.VOICE
                        else -> DialTool.SETTINGS
                    }
                    onToolSelected(tool)
                    targetRotation = tool.ordinal * 90f
                    accumulatedAngle = tool.ordinal * 90f
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width
            val centerY = size.height
            val radiusPx = dialRadius.toPx()

            rotate(animatedRotation.value, pivot = Offset(centerX, centerY)) {
                // Outer ring
                drawCircle(
                    color = SlateDeep,
                    radius = radiusPx,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 6.dp.toPx())
                )

                // Inner disk fill
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Parchment, ParchmentDark, Sandstone),
                        center = Offset(centerX, centerY),
                        radius = radiusPx
                    ),
                    radius = radiusPx - 4.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Inner ring accent
                drawCircle(
                    color = ClayWarm,
                    radius = radiusPx * 0.7f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Concentric texture rings
                for (i in 1..4) {
                    drawCircle(
                        color = ClayDark.copy(alpha = 0.15f),
                        radius = radiusPx * (0.3f + i * 0.15f),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Tool segment dividers
                for (i in 0..3) {
                    val dividerAngle = Math.toRadians((i * 90.0))
                    val endX = centerX + radiusPx * cos(dividerAngle).toFloat()
                    val endY = centerY + radiusPx * sin(dividerAngle).toFloat()
                    drawLine(
                        color = SlateMedium.copy(alpha = 0.4f),
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Tool indicator dot
                    val dotAngle = Math.toRadians((i * 90.0 + 45.0))
                    val dotRadius = radiusPx * 0.5f
                    val dotX = centerX + dotRadius * cos(dotAngle).toFloat()
                    val dotY = centerY + dotRadius * sin(dotAngle).toFloat()
                    val isSelected = DialTool.entries[i] == currentTool

                    drawCircle(
                        color = if (isSelected) RustAccent else ClayDark,
                        radius = if (isSelected) 12.dp.toPx() else 8.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                // Center hub
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ClayWarm, ClayDark),
                        center = Offset(centerX, centerY),
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Center dot
                drawCircle(
                    color = InkBrown,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }

            // Current tool indicator (fixed, doesn't rotate)
            val indicatorAngle = Math.toRadians(-135.0)
            val indicatorRadius = radiusPx * 0.35f
            val indicatorX = centerX + indicatorRadius * cos(indicatorAngle).toFloat()
            val indicatorY = centerY + indicatorRadius * sin(indicatorAngle).toFloat()
            drawCircle(
                color = RustAccent,
                radius = 6.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }

        // Current tool label overlay in the visible portion
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp)
        ) {
            Text(
                text = currentTool.symbol + " " + currentTool.label,
                style = TextStyle(
                    color = RustAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun Float.toDegrees(): Float = Math.toDegrees(this.toDouble()).toFloat()
