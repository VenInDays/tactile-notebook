package com.tactile.notebook.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tactile.notebook.TactileNotebookApp
import com.tactile.notebook.data.entity.NoteEntity
import com.tactile.notebook.data.repository.NoteRepository
import com.tactile.notebook.spatial.SpatialSearchEngine
import com.tactile.notebook.ui.components.DialTool
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotebookState(
    val notes: List<NoteEntity> = emptyList(),
    val archivedNotes: List<NoteEntity> = emptyList(),
    val selectedNoteId: Long? = null,
    val currentTool: DialTool = DialTool.PEN,
    val isEditing: Boolean = false,
    val spatialSearch: SpatialSearchEngine = SpatialSearchEngine(),
    val searchQuery: String = ""
)

class NotebookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(
        (application as TactileNotebookApp).database.noteDao()
    )

    private val _state = MutableStateFlow(NotebookState())
    val state: StateFlow<NotebookState> = _state.asStateFlow()

    init {
        // Load active notes
        viewModelScope.launch {
            repository.getActiveNotes().collect { notes ->
                _state.update { it.copy(notes = notes) }
            }
        }

        // Seed some demo notes if empty
        viewModelScope.launch {
            val existing = repository.getActiveNotes().first()
            if (existing.isEmpty()) {
                seedDemoNotes()
            }
        }
    }

    fun selectTool(tool: DialTool) {
        _state.update { it.copy(currentTool = tool) }
    }

    fun selectNote(id: Long?) {
        _state.update { it.copy(selectedNoteId = id, isEditing = id != null) }
    }

    fun createNote() {
        viewModelScope.launch {
            val tool = _state.value.currentTool
            repository.createNote(
                title = "New ${tool.label} Note",
                content = "",
                toolUsed = tool.name.lowercase()
            )
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun archiveNote(id: Long) {
        viewModelScope.launch {
            repository.archiveNote(id)
            if (_state.value.selectedNoteId == id) {
                _state.update { it.copy(selectedNoteId = null, isEditing = false) }
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(pinned = !note.pinned))
        }
    }

    fun spatialPan(delta: androidx.compose.ui.geometry.Offset) {
        _state.value.spatialSearch.pan(delta)
    }

    fun spatialFling(vx: Float, vy: Float) {
        _state.value.spatialSearch.fling(vx, vy)
    }

    fun spatialZoom(delta: Float, focus: androidx.compose.ui.geometry.Offset) {
        _state.value.spatialSearch.zoom(delta, focus)
    }

    private suspend fun seedDemoNotes() {
        val demoNotes = listOf(
            Triple("Morning Thoughts", "The light filters through the window like watercolor paint, spreading amber across the desk. Ideas form in the spaces between coffee sips.", "pen"),
            Triple("Project Sketch", "Layout concept for the organic UI — circular forms that breathe, edges that crumble like dried clay. No rectangles.", "camera"),
            Triple("Voice Memo", "Recorded at 3pm: The sound of rain on tin roof, layered with distant traffic. Sample for ambient track.", "voice"),
            Triple("Reading Notes", "Chapter 7 discusses the tension between structure and chaos in design. The author argues for controlled imperfection.", "pen"),
            Triple("Recipe", "Sourdough: 500g flour, 350ml water, 100g starter, 10g salt. Mix, fold every 30 min for 4 hours.", "pen"),
        )

        demoNotes.forEach { (title, content, tool) ->
            repository.createNote(
                title = title,
                content = content,
                toolUsed = tool
            )
        }
    }
}
