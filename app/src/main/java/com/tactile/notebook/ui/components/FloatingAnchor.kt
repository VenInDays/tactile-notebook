package com.tactile.notebook.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tactile.notebook.data.entity.NoteEntity
import com.tactile.notebook.ui.theme.*

/**
 * "The Floating Anchor" — A floating element that sits 20% over the edge
 * of the active note card, acting as a dynamic shortcut bar.
 */
data class AnchorAction(
    val label: String,
    val symbol: String,
    val action: () -> Unit
)

@Composable
fun FloatingAnchor(
    modifier: Modifier = Modifier,
    activeNote: NoteEntity?,
    actions: List<AnchorAction> = defaultAnchorActions(),
    onAction: (Int) -> Unit = {},
    onAnchorDrag: (Offset) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    var anchorOffsetX by remember { mutableFloatStateOf(0f) }
    var anchorOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    val pulseAlpha = rememberInfiniteTransition(label = "anchor_pulse")
    val pulse by pulseAlpha.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(anchorOffsetX) {
        animatedOffsetX.animateTo(
            anchorOffsetX,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
        )
    }
    LaunchedEffect(anchorOffsetY) {
        animatedOffsetY.animateTo(
            anchorOffsetY,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    anchorOffsetX += dragAmount.x
                    anchorOffsetY += dragAmount.y
                    onAnchorDrag(dragAmount)
                    change.consume()
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(actions.size) {
                    detectTapGestures { offset ->
                        val anchorWidthPx = (actions.size * 52.dp).toPx()
                        val anchorHeightPx = 44.dp.toPx()
                        val anchorX = size.width - anchorWidthPx * 0.8f + animatedOffsetX.value
                        val anchorY = size.height * 0.55f + animatedOffsetY.value

                        val tapIndex = ((offset.x - anchorX) / 52.dp.toPx()).toInt()
                        if (tapIndex in actions.indices) {
                            onAction(tapIndex)
                        }
                    }
                }
        ) {
            if (activeNote == null) return@Canvas

            val anchorWidthPx = (actions.size * 52.dp).toPx()
            val anchorHeightPx = 44.dp.toPx()
            val anchorX = size.width - anchorWidthPx * 0.8f + animatedOffsetX.value
            val anchorY = size.height * 0.55f + animatedOffsetY.value

            // Shadow
            drawRoundRect(
                color = CharcoalSoft.copy(alpha = 0.15f),
                topLeft = Offset(anchorX + 2.dp.toPx(), anchorY + 4.dp.toPx()),
                size = Size(anchorWidthPx, anchorHeightPx),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            // Main body
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        SlateDeep.copy(alpha = 0.92f),
                        CharcoalSoft.copy(alpha = 0.88f)
                    )
                ),
                topLeft = Offset(anchorX, anchorY),
                size = Size(anchorWidthPx, anchorHeightPx),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            // Border
            drawRoundRect(
                color = ClayWarm.copy(alpha = pulse * 0.6f),
                topLeft = Offset(anchorX, anchorY),
                size = Size(anchorWidthPx, anchorHeightPx),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Left edge tab
            drawRoundRect(
                color = RustAccent.copy(alpha = 0.9f),
                topLeft = Offset(anchorX - 6.dp.toPx(), anchorY + 8.dp.toPx()),
                size = Size(6.dp.toPx(), anchorHeightPx - 16.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            // Action icons
            actions.forEachIndexed { index, action ->
                val iconX = anchorX + 12.dp.toPx() + index * 52.dp.toPx()
                val iconY = anchorY + (anchorHeightPx - 20.sp.toPx()) / 2f

                if (index > 0) {
                    drawLine(
                        color = ClayWarm.copy(alpha = 0.3f),
                        start = Offset(iconX - 8.dp.toPx(), anchorY + 10.dp.toPx()),
                        end = Offset(iconX - 8.dp.toPx(), anchorY + anchorHeightPx - 10.dp.toPx()),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                val symbolResult = textMeasurer.measure(
                    text = AnnotatedString(action.symbol),
                    style = TextStyle(
                        color = CreamLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                drawText(
                    textLayoutResult = symbolResult,
                    topLeft = Offset(
                        iconX + (40.dp.toPx() - symbolResult.size.width) / 2f,
                        iconY
                    )
                )

                val labelResult = textMeasurer.measure(
                    text = AnnotatedString(action.label),
                    style = TextStyle(
                        color = Sandstone.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        iconX + (40.dp.toPx() - labelResult.size.width) / 2f,
                        anchorY + anchorHeightPx - 14.dp.toPx()
                    )
                )
            }
        }
    }
}

fun defaultAnchorActions() = listOf(
    AnchorAction("Edit", "\u270E") {},
    AnchorAction("Pin", "\u25C6") {},
    AnchorAction("Move", "\u2725") {},
    AnchorAction("Del", "\u2716") {}
)
