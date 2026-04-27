package com.tactile.notebook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun TactileNotebookApp(
    viewModel: NotebookViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Background canvas texture
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                val selectedNote = state.notes.find { it.id == state.selectedNoteId }
                NoteEditorScreen(
                    note = selectedNote,
                    onNoteChange = { viewModel.updateNote(it) },
                    onBack = { viewModel.selectNote(null) }
                )
            }
            else -> {
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
                        0 -> { viewModel.selectNote(activeNote?.id ?: return@FloatingAnchor) }
                        1 -> { activeNote?.let { viewModel.togglePin(it) } }
                        2 -> { /* Move — future */ }
                        3 -> { activeNote?.let { viewModel.deleteNote(it) } }
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

        // Status bar area — app title using Compose Text instead of Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopStart)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = SlateDeep.copy(alpha = 0.85f),
                    size = Size(size.width, size.height)
                )
                drawLine(
                    color = ClayWarm,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TACTILE NOTEBOOK",
                    style = TextStyle(
                        color = Parchment,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = state.currentTool.label.uppercase(),
                    style = TextStyle(
                        color = RustAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        // Note count indicator (bottom-left)
        Text(
            text = "${state.notes.size} notes",
            style = TextStyle(
                color = SlateMedium.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 16.dp)
        )
    }
}
