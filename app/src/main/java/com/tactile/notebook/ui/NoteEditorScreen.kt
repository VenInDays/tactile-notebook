package com.tactile.notebook.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.brush.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tactile.notebook.data.entity.NoteEntity
import com.tactile.notebook.ui.theme.*

/**
 * Note Editor Screen — Organic brutalist style text editor.
 * No standard text fields. Raw, tactile editing experience.
 */
@Composable
fun NoteEditorScreen(
    note: NoteEntity?,
    onNoteChange: (NoteEntity) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var titleState by remember(note?.id) {
        mutableStateOf(TextFieldValue(note?.title ?: ""))
    }
    var contentState by remember(note?.id) {
        mutableStateOf(TextFieldValue(note?.content ?: ""))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Background texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Paper grain texture
            for (i in 0..20) {
                val y = i * size.height / 20
                drawLine(
                    color = SlateMedium.copy(alpha = 0.04f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            // Margin line
            drawLine(
                color = ClayWarm.copy(alpha = 0.2f),
                start = Offset(60.dp.toPx(), 0f),
                end = Offset(60.dp.toPx(), size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(start = 44.dp) // After margin line
        ) {
            // Back indicator — tactile "fold" mark
            Canvas(modifier = Modifier.size(32.dp, 32.dp)) {
                drawLine(
                    color = ClayWarm,
                    start = Offset(8.dp.toPx(), 0f),
                    end = Offset(0f, 8.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = SlateDeep,
                    start = Offset(0f, 8.dp.toPx()),
                    end = Offset(24.dp.toPx(), 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title field
            BasicTextField(
                value = titleState,
                onValueChange = { newValue ->
                    titleState = newValue
                    note?.let {
                        onNoteChange(it.copy(title = newValue.text))
                    }
                },
                textStyle = TextStyle(
                    color = InkBrown,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (titleState.text.isEmpty()) {
                        androidx.compose.foundation.layout.Box {
                            androidx.compose.material3.Text(
                                text = "Title",
                                color = SlateMedium.copy(alpha = 0.4f),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Divider line
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)) {
                drawLine(
                    color = ClayWarm.copy(alpha = 0.5f),
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content field
            BasicTextField(
                value = contentState,
                onValueChange = { newValue ->
                    contentState = newValue
                    note?.let {
                        onNoteChange(it.copy(content = newValue.text))
                    }
                },
                textStyle = TextStyle(
                    color = SlateDeep,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                decorationBox = { innerTextField ->
                    if (contentState.text.isEmpty()) {
                        androidx.compose.foundation.layout.Box {
                            androidx.compose.material3.Text(
                                text = "Write something...",
                                color = SlateMedium.copy(alpha = 0.3f),
                                fontSize = 15.sp
                            )
                        }
                    }
                    innerTextField()
                }
            )
        }
    }
}
