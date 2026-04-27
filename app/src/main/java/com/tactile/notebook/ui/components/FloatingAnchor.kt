package com.tactile.notebook.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

    if (activeNote == null) return

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
        // Anchor bar — using Compose layout instead of Canvas for text
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = animatedOffsetX.value.dp, y = animatedOffsetY.value.dp)
                .padding(end = 8.dp)
                .background(
                    color = SlateDeep.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left edge tab indicator
            Canvas(modifier = Modifier.size(6.dp, 28.dp)) {
                drawRoundRect(
                    color = RustAccent,
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            actions.forEachIndexed { index, action ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onAction(index) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = action.symbol,
                        style = TextStyle(
                            color = CreamLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = action.label,
                        style = TextStyle(
                            color = Sandstone.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                if (index < actions.size - 1) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Canvas(modifier = Modifier.size(1.dp, 24.dp)) {
                        drawLine(
                            color = ClayWarm.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
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
