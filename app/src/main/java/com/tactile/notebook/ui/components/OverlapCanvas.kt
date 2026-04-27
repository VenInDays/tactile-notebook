package com.tactile.notebook.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    // Track drag state
    var draggingCardId by remember { mutableLongStateOf(-1L) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(notes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingCardId = -1L // Simplified: always canvas drag for now
                    },
                    onDrag = { change, dragAmount ->
                        canvasOffset += dragAmount
                        onCanvasDrag(dragAmount)
                        change.consume()
                    },
                    onDragEnd = {
                        // No-op for canvas drag
                    },
                    onDragCancel = {
                        canvasOffset = Offset.Zero
                    }
                )
            }
    ) {
        // Background dot grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (x in 0..(size.width.toInt()) step 60) {
                for (y in 0..(size.height.toInt()) step 60) {
                    drawCircle(
                        color = ClayDark.copy(alpha = 0.08f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(
                            x.toFloat() + canvasOffset.x % 60,
                            y.toFloat() + canvasOffset.y % 60
                        )
                    )
                }
            }
        }

        // Stacked cards using Compose layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notes.forEachIndexed { index, note ->
                val cardColor = parseNoteColor(note.color)
                val isSelected = note.id == selectedNoteId
                val rotationDegrees = note.rotation

                NoteCard(
                    note = note,
                    cardColor = cardColor,
                    isSelected = isSelected,
                    rotationDegrees = rotationDegrees,
                    onClick = { onSelect(note.id) },
                    onArchive = { onArchive(note.id) }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    cardColor: Color,
    isSelected: Boolean,
    rotationDegrees: Float,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    // Physics-based fling state
    var flingOffset by remember { mutableStateOf(Offset.Zero) }
    var isFlinging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = rotationDegrees + (flingOffset.x / 50f)
                translationX = flingOffset.x
                translationY = flingOffset.y
                shadowElevation = if (isSelected) 8.dp.toPx() else 4.dp.toPx()
                shape = RoundedCornerShape(8.dp)
                clip = true
            }
            .background(
                color = if (isFlinging) RustAccent.copy(alpha = 0.3f) else cardColor,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(note.id) {
                detectDragGestures(
                    onDragStart = {
                        flingOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        flingOffset += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        // Check if flung to top-right (archive zone)
                        if (flingOffset.y < -150f && flingOffset.x > 80f) {
                            isFlinging = true
                            onArchive()
                        } else {
                            flingOffset = Offset.Zero
                        }
                    },
                    onDragCancel = {
                        flingOffset = Offset.Zero
                    }
                )
            }
            .pointerInput(note.id) {
                detectTapGestures { onClick() }
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            // Title
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = TextStyle(
                    color = InkBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content preview
            if (note.content.isNotEmpty()) {
                Text(
                    text = note.content.take(120),
                    style = TextStyle(
                        color = SlateMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: tool indicator + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val toolSymbol = when (note.toolUsed) {
                    "camera" -> DialTool.CAMERA.symbol
                    "voice" -> DialTool.VOICE.symbol
                    else -> DialTool.PEN.symbol
                }
                Text(
                    text = toolSymbol,
                    style = TextStyle(
                        color = ClayDark.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = formatTimestamp(note.timestamp),
                    style = TextStyle(
                        color = SlateMedium.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }

            // Archive overlay when flinging
            if (isFlinging || (flingOffset.y < -150f && flingOffset.x > 80f)) {
                Text(
                    text = "ARCHIVE",
                    style = TextStyle(
                        color = RustAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Pin indicator
            if (note.pinned) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                ) {
                    drawCircle(color = RustAccent, radius = 4.dp.toPx())
                }
            }
        }
    }
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
