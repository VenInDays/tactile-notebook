package com.tactile.notebook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tactile.notebook.data.entity.NoteEntity
import com.tactile.notebook.ui.components.DialTool
import com.tactile.notebook.ui.components.FloatingAnchor
import com.tactile.notebook.ui.components.KineticDial
import com.tactile.notebook.ui.components.OverlapCanvas
import com.tactile.notebook.ui.theme.TactileNotebookTheme
import com.tactile.notebook.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TactileNotebookTheme {
                TactileNotebookApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TactileNotebookApp(
    viewModel: NotebookViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Background canvas texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle dot grid for spatial reference
            for (x in 0..size.width.toInt() step 40) {
                for (y in 0..size.height.toInt() step 40) {
                    drawCircle(
                        color = ClayDark.copy(alpha = 0.05f),
                        radius = 1.dp.toPx(),
                        center = Offset(x.toFloat(), y.toFloat())
                    )
                }
            }
        }

        // Main content area
        when {
            state.isEditing && state.selectedNoteId != null -> {
                // Note editor mode
                val selectedNote = state.notes.find { it.id == state.selectedNoteId }
                NoteEditorScreen(
                    note = selectedNote,
                    onNoteChange = { viewModel.updateNote(it) },
                    onBack = { viewModel.selectNote(null) }
                )
            }
            else -> {
                // Canvas mode — overlap cards view
                OverlapCanvas(
                    modifier = Modifier.fillMaxSize(),
                    notes = state.notes,
                    onArchive = { viewModel.archiveNote(it) },
                    onSelect = { viewModel.selectNote(it) },
                    onCanvasDrag = { viewModel.spatialPan(it) },
                    selectedNoteId = state.selectedNoteId
                )
            }
        }

        // Floating Anchor (only in canvas mode)
        if (!state.isEditing) {
            val activeNote = state.notes.firstOrNull()
            FloatingAnchor(
                modifier = Modifier.fillMaxSize(),
                activeNote = activeNote,
                onAction = { index ->
                    when (index) {
                        0 -> { /* Edit */ viewModel.selectNote(activeNote?.id ?: return@FloatingAnchor) }
                        1 -> { /* Pin */ activeNote?.let { viewModel.togglePin(it) } }
                        2 -> { /* Move — future drag feature */ }
                        3 -> { /* Delete */ activeNote?.let { viewModel.deleteNote(it) } }
                    }
                }
            )
        }

        // Kinetic Dial — positioned at bottom-right, only 25% visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 250.dp, y = 250.dp)
                .align(Alignment.BottomEnd)
        ) {
            KineticDial(
                modifier = Modifier.size(500.dp),
                onToolSelected = { tool ->
                    viewModel.selectTool(tool)
                    if (tool != DialTool.SETTINGS) {
                        viewModel.createNote()
                    }
                },
                currentTool = state.currentTool
            )
        }

        // Status bar area — app title
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopStart)
        ) {
            // Title background
            drawRect(
                color = SlateDeep.copy(alpha = 0.85f),
                size = Size(size.width, 48.dp.toPx())
            )

            // Title text
            val titleResult = textMeasurer.measure(
                text = AnnotatedString("TACTILE NOTEBOOK"),
                style = TextStyle(
                    color = Parchment,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            drawText(
                textLayoutResult = titleResult,
                topLeft = Offset(20.dp.toPx(), (48.dp.toPx() - titleResult.size.height) / 2f)
            )

            // Tool indicator
            val toolLabel = state.currentTool.label.uppercase()
            val toolResult = textMeasurer.measure(
                text = AnnotatedString(toolLabel),
                style = TextStyle(
                    color = RustAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            )
            drawText(
                textLayoutResult = toolResult,
                topLeft = Offset(
                    size.width - 20.dp.toPx() - toolResult.size.width,
                    (48.dp.toPx() - toolResult.size.height) / 2f
                )
            )

            // Bottom accent line
            drawLine(
                color = ClayWarm,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Note count indicator (bottom-left)
        Canvas(
            modifier = Modifier
                .size(80.dp, 32.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp, start = 8.dp)
        ) {
            val countText = "${state.notes.size} notes"
            val countResult = textMeasurer.measure(
                text = AnnotatedString(countText),
                style = TextStyle(
                    color = SlateMedium.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            drawText(
                textLayoutResult = countResult,
                topLeft = Offset(8.dp.toPx(), (size.height - countResult.size.height) / 2f)
            )
        }
    }
}
