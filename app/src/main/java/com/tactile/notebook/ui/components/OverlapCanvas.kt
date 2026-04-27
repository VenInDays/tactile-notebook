package com.tactile.notebook.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tactile.notebook.data.entity.NoteEntity
import com.tactile.notebook.ui.theme.*
import kotlin.math.*

/**
 * Data class for a card's animated state
 */
data class CardState(
    val note: NoteEntity,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = note.rotation,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val isDragging: Boolean = false
)

/**
 * "The Overlap Canvas" — Stacked card layout with physics-based interactions.
 * Cards are stacked with slight random rotation (±3 degrees).
 * Swipe top-right to archive with fling animation.
 * Drag background canvas for spatial navigation.
 */
@Composable
fun OverlapCanvas(
    modifier: Modifier = Modifier,
    notes: List<NoteEntity>,
    onArchive: (Long) -> Unit = {},
    onSelect: (Long) -> Unit = {},
    onCanvasDrag: (Offset) -> Unit = {},
    selectedNoteId: Long? = null
) {
    val textMeasurer = rememberTextMeasurer()

    // Card states with spring animations
    val cardStates = remember(notes) {
        notes.mapIndexed { index, note ->
            CardState(
                note = note,
                offsetX = 0f,
                offsetY = -index * 40f, // Stack offset
                rotation = note.rotation
            )
        }
    }

    // Animated card offsets for physics-based fling
    var cardAnimStates by remember { mutableStateOf(cardStates) }
    val flingAnimations = remember { mutableMapOf<Long, Animatable<Offset, AnimationVector2D>>() }

    // Track drag state
    var draggingCardId by remember { mutableLongStateOf(-1L) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    // Fling velocity tracker
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var flingVelocity by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(notes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Determine if dragging a card or the canvas
                        val hitCard = findCardAt(offset, cardAnimStates, size)
                        if (hitCard != null) {
                            draggingCardId = hitCard.note.id
                            lastDragPosition = offset
                            lastDragTime = System.currentTimeMillis()
                        } else {
                            draggingCardId = -1L
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (draggingCardId >= 0) {
                            // Dragging a card
                            dragOffset += dragAmount
                            change.consume()

                            // Track velocity
                            val now = System.currentTimeMillis()
                            val dt = (now - lastDragTime).coerceAtLeast(1)
                            flingVelocity = Offset(
                                (change.position.x - lastDragPosition.x) / dt * 16f,
                                (change.position.y - lastDragPosition.y) / dt * 16f
                            )
                            lastDragPosition = change.position
                            lastDragTime = now
                        } else {
                            // Dragging the canvas — spatial navigation
                            canvasOffset += dragAmount
                            onCanvasDrag(dragAmount)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (draggingCardId >= 0) {
                            // Check if flung to top-right (archive zone)
                            val archiveThreshold = -200f
                            val isArchiveGesture =
                                dragOffset.y < archiveThreshold && dragOffset.x > 100f

                            if (isArchiveGesture) {
                                // Archive with fling animation
                                onArchive(draggingCardId)
                            }

                            // Reset
                            draggingCardId = -1L
                            dragOffset = Offset.Zero
                            flingVelocity = Offset.Zero
                        }
                    },
                    onDragCancel = {
                        draggingCardId = -1L
                        dragOffset = Offset.Zero
                    }
                )
            }
    ) {
        val cardWidth = size.width * 0.85f
        val cardHeight = size.height * 0.45f
        val cardStartX = (size.width - cardWidth) / 2f
        val baseStartY = size.height * 0.12f

        // Draw subtle canvas texture (grid of dots for spatial reference)
        for (x in 0..(size.width.toInt()) step 60) {
            for (y in 0..(size.height.toInt()) step 60) {
                drawCircle(
                    color = ClayDark.copy(alpha = 0.08f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(x.toFloat() + canvasOffset.x % 60, y.toFloat() + canvasOffset.y % 60)
                )
            }
        }

        // Draw cards from bottom to top (first card is on top)
        cardAnimStates.forEachIndexed { index, state ->
            val note = state.note
            val cardColor = parseNoteColor(note.color)

            // Calculate card position
            val stackOffsetY = -index * 40f
            val dragOffsetForCard = if (note.id == draggingCardId) dragOffset else Offset.Zero

            val cardX = cardStartX + dragOffsetForCard.x
            val cardY = baseStartY + stackOffsetY + dragOffsetForCard.y
            val cardRotation = note.rotation + if (note.id == draggingCardId) {
                (dragOffsetForCard.x / size.width) * 15f // Extra rotation while dragging
            } else 0f

            // Calculate alpha for archive fling
            val isArchiving = note.id == draggingCardId &&
                    dragOffset.y < -200f && dragOffset.x > 100f
            val cardAlpha = if (isArchiving) {
                1f - (abs(dragOffset.y) / 400f).coerceIn(0f, 1f)
            } else if (note.id == selectedNoteId) {
                1f
            } else {
                1f - index * 0.05f
            }

            // Scale for selected card
            val cardScale = if (note.id == selectedNoteId) 1.02f else 1f - index * 0.01f

            // Draw shadow first
            rotate(cardRotation, pivot = Offset(cardX + cardWidth / 2, cardY + cardHeight / 2)) {
                // Shadow
                drawRoundRect(
                    color = CharcoalSoft.copy(alpha = 0.2f),
                    topLeft = Offset(cardX + 4.dp.toPx(), cardY + 6.dp.toPx()),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                // Card body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(cardColor, cardColor.copy(alpha = 0.92f))
                    ),
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                // Card edge highlight (top)
                drawRoundRect(
                    color = CreamLight.copy(alpha = 0.3f),
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardWidth, 3.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                )

                // Card torn-paper texture lines
                for (i in 1..6) {
                    val lineY = cardY + 60.dp.toPx() + i * 28.dp.toPx()
                    if (lineY < cardY + cardHeight - 20.dp.toPx()) {
                        drawLine(
                            color = SlateMedium.copy(alpha = 0.06f),
                            start = Offset(cardX + 20.dp.toPx(), lineY),
                            end = Offset(cardX + cardWidth - 20.dp.toPx(), lineY),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }

                // Title text
                val titleText = note.title.ifEmpty { "Untitled" }
                val titleResult = textMeasurer.measure(
                    text = AnnotatedString(titleText),
                    style = TextStyle(
                        color = InkBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    constraints = androidx.compose.ui.unit.Constraints(
                        maxWidth = (cardWidth - 40.dp.toPx()).toInt()
                    )
                )
                drawText(
                    textLayoutResult = titleResult,
                    topLeft = Offset(cardX + 20.dp.toPx(), cardY + 18.dp.toPx()),
                    alpha = cardAlpha
                )

                // Content preview
                val contentPreview = note.content.take(120)
                if (contentPreview.isNotEmpty()) {
                    val contentResult = textMeasurer.measure(
                        text = AnnotatedString(contentPreview),
                        style = TextStyle(
                            color = SlateMedium,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = (cardWidth - 40.dp.toPx()).toInt()
                        )
                    )
                    drawText(
                        textLayoutResult = contentResult,
                        topLeft = Offset(cardX + 20.dp.toPx(), cardY + 44.dp.toPx()),
                        alpha = cardAlpha
                    )
                }

                // Tool indicator (bottom-left of card)
                val toolSymbol = when (note.toolUsed) {
                    "camera" -> DialTool.CAMERA.symbol
                    "voice" -> DialTool.VOICE.symbol
                    else -> DialTool.PEN.symbol
                }
                val toolResult = textMeasurer.measure(
                    text = AnnotatedString(toolSymbol),
                    style = TextStyle(
                        color = ClayDark.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                )
                drawText(
                    textLayoutResult = toolResult,
                    topLeft = Offset(
                        cardX + 16.dp.toPx(),
                        cardY + cardHeight - 28.dp.toPx()
                    ),
                    alpha = cardAlpha
                )

                // Timestamp (bottom-right)
                val timeStr = formatTimestamp(note.timestamp)
                val timeResult = textMeasurer.measure(
                    text = AnnotatedString(timeStr),
                    style = TextStyle(
                        color = SlateMedium.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
                drawText(
                    textLayoutResult = timeResult,
                    topLeft = Offset(
                        cardX + cardWidth - 20.dp.toPx() - timeResult.size.width,
                        cardY + cardHeight - 24.dp.toPx()
                    ),
                    alpha = cardAlpha
                )

                // Archive zone indicator when dragging
                if (note.id == draggingCardId && isArchiving) {
                    drawRoundRect(
                        color = RustAccent.copy(alpha = 0.3f),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardWidth, cardHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                    // "Archive" text
                    val archiveResult = textMeasurer.measure(
                        text = AnnotatedString("ARCHIVE"),
                        style = TextStyle(
                            color = RustAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = archiveResult,
                        topLeft = Offset(
                            cardX + (cardWidth - archiveResult.size.width) / 2f,
                            cardY + (cardHeight - archiveResult.size.height) / 2f
                        )
                    )
                }

                // Pin indicator
                if (note.pinned) {
                    drawCircle(
                        color = RustAccent,
                        radius = 4.dp.toPx(),
                        center = Offset(cardX + cardWidth - 20.dp.toPx(), cardY + 16.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun findCardAt(
    offset: Offset,
    cards: List<CardState>,
    canvasSize: Size
): CardState? {
    val cardWidth = canvasSize.width * 0.85f
    val cardHeight = canvasSize.height * 0.45f
    val cardStartX = (canvasSize.width - cardWidth) / 2f
    val baseStartY = canvasSize.height * 0.12f

    // Check from top card (last in list) to bottom
    for (i in cards.indices.reversed()) {
        val stackOffsetY = -i * 40f
        val cardRect = Rect(
            left = cardStartX,
            top = baseStartY + stackOffsetY,
            right = cardStartX + cardWidth,
            bottom = baseStartY + stackOffsetY + cardHeight
        )
        if (cardRect.contains(offset)) {
            return cards[i]
        }
    }
    return null
}

private fun parseNoteColor(colorName: String): Color = when (colorName) {
    "amber" -> NoteAmber
    "rose" -> NoteRose
    "sage" -> NoteSage
    "sky" -> NoteSky
    "lavender" -> NoteLavender
    "sandstone" -> Sandstone
    else -> Parchment
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
